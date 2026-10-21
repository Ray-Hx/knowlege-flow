import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.knowledgeflow.KnowledgeFlowApplication;
import com.ray.knowledgeflow.module.ai.service.Reranker;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 本地 RAG 召回评测：同一批 Qdrant candidates 分别比较向量顺序和 CrossEncoder 顺序。
 * 该类放在 scripts 下，只作为开发评测工具，不属于生产运行时。
 */
public final class RagBenchmarkRunner {

    private static final String COLLECTION = "content_chunks";
    private static final long USER_ID = 1L;
    private static final int CANDIDATE_SIZE = 20;
    private static final int TOP_K = 5;

    private RagBenchmarkRunner() {
    }

    public static void main(String[] args) throws Exception {
        Path casesPath = Path.of(args.length > 0 ? args[0] : "scripts/rag_corpus_eval_cases.json");
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> cases = new ArrayList<>();
        mapper.readTree(casesPath.toFile()).forEach(cases::add);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(KnowledgeFlowApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.main.banner-mode=off",
                        "spring.main.web-application-type=none",
                        "server.port=0",
                        "spring.quartz.auto-startup=false")
                .run()) {
            EmbeddingService embeddingService = context.getBean(EmbeddingService.class);
            QdrantClient qdrantClient = context.getBean(QdrantClient.class);
            Reranker reranker = context.getBean(Reranker.class);

            List<String> queries = cases.stream().map(c -> c.path("query").asText()).toList();
            List<List<Float>> vectors = embeddingService.embedAll(queries);
            if (vectors.size() != queries.size()) {
                throw new IllegalStateException("embedding 返回数量不匹配: " + vectors.size() + "/" + queries.size());
            }

            List<Result> results = new ArrayList<>();
            for (int i = 0; i < cases.size(); i++) {
                JsonNode testCase = cases.get(i);
                List<Points.ScoredPoint> candidates = search(qdrantClient, vectors.get(i));
                List<String> candidateTexts = candidates.stream().map(RagBenchmarkRunner::content).toList();
                List<Integer> reranked = reranker.rerank(testCase.path("query").asText(), candidateTexts, TOP_K);
                results.add(evaluate(testCase, candidates, reranked));
                System.out.printf("%s vector=%s rerank=%s%n", testCase.path("id").asText(),
                        titles(candidates, firstIndices(candidates.size())), titles(candidates, reranked));
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, summary(results));
            System.out.println();
        }
    }

    private static List<Points.ScoredPoint> search(QdrantClient client, List<Float> vector)
            throws ExecutionException, InterruptedException {
        Points.SearchPoints request = Points.SearchPoints.newBuilder()
                .setCollectionName(COLLECTION)
                .addAllVector(vector)
                .setLimit(CANDIDATE_SIZE)
                .setFilter(userFilter(USER_ID))
                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                .setScoreThreshold(0.5f)
                .build();
        return client.searchAsync(request).get();
    }

    private static Points.Filter userFilter(long userId) {
        return Points.Filter.newBuilder()
                .addMust(Points.Condition.newBuilder()
                        .setField(Points.FieldCondition.newBuilder()
                                .setKey("userId")
                                .setMatch(Points.Match.newBuilder().setInteger(userId).build())
                                .build())
                        .build())
                .build();
    }

    private static Result evaluate(JsonNode testCase, List<Points.ScoredPoint> candidates,
            List<Integer> reranked) {
        Set<String> gold = new HashSet<>();
        testCase.path("gold_titles").forEach(n -> gold.add(normalize(n.asText())));
        List<Integer> vectorTop = firstIndices(candidates.size());
        return new Result(
                testCase.path("id").asText(),
                testCase.path("category").asText(),
                gold.isEmpty(),
                hitAny(candidates, vectorTop, gold),
                hitAny(candidates, reranked, gold),
                allGold(candidates, vectorTop, gold),
                allGold(candidates, reranked, gold),
                reciprocalRank(candidates, vectorTop, gold),
                reciprocalRank(candidates, reranked, gold));
    }

    private static boolean hitAny(List<Points.ScoredPoint> candidates, List<Integer> indices, Set<String> gold) {
        if (gold.isEmpty()) {
            return false;
        }
        return indices.stream().anyMatch(i -> gold.contains(normalize(title(candidates.get(i)))));
    }

    private static boolean allGold(List<Points.ScoredPoint> candidates, List<Integer> indices, Set<String> gold) {
        if (gold.isEmpty()) {
            return false;
        }
        Set<String> found = indices.stream().map(i -> normalize(title(candidates.get(i))))
                .filter(gold::contains).collect(Collectors.toSet());
        return found.containsAll(gold);
    }

    private static double reciprocalRank(List<Points.ScoredPoint> candidates, List<Integer> indices, Set<String> gold) {
        if (gold.isEmpty()) {
            return 0.0;
        }
        for (int rank = 0; rank < indices.size(); rank++) {
            if (gold.contains(normalize(title(candidates.get(indices.get(rank)))))) {
                return 1.0 / (rank + 1);
            }
        }
        return 0.0;
    }

    private static JsonNode summary(List<Result> results) {
        ObjectMapper mapper = new ObjectMapper();
        long answerable = results.stream().filter(r -> !r.unanswerable).count();
        return mapper.createObjectNode()
                .put("cases", results.size())
                .put("answerable_cases", answerable)
                .put("vector_hit_any_at_5", count(results, false, true, false))
                .put("rerank_hit_any_at_5", count(results, true, true, false))
                .put("vector_all_gold_at_5", count(results, false, false, true))
                .put("rerank_all_gold_at_5", count(results, true, false, true))
                .put("vector_mrr_at_5", average(results, false))
                .put("rerank_mrr_at_5", average(results, true));
    }

    private static int count(List<Result> results, boolean rerank, boolean any, boolean all) {
        return (int) results.stream().filter(r -> !r.unanswerable).filter(r ->
                all ? (rerank ? r.rerankAll : r.vectorAll) : (rerank ? r.rerankAny : r.vectorAny)).count();
    }

    private static double average(List<Result> results, boolean rerank) {
        return Math.round(results.stream().filter(r -> !r.unanswerable)
                .mapToDouble(r -> rerank ? r.rerankMrr : r.vectorMrr).average().orElse(0.0) * 1000.0) / 1000.0;
    }

    private static List<Integer> firstIndices(int size) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_K, size); i++) {
            indices.add(i);
        }
        return indices;
    }

    private static String titles(List<Points.ScoredPoint> candidates, List<Integer> indices) {
        return indices.stream().map(i -> title(candidates.get(i))).toList().toString();
    }

    private static String content(Points.ScoredPoint point) {
        return point.getPayloadMap().containsKey("content")
                ? point.getPayloadMap().get("content").getStringValue() : "";
    }

    private static String title(Points.ScoredPoint point) {
        return point.getPayloadMap().containsKey("title")
                ? point.getPayloadMap().get("title").getStringValue() : "";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private record Result(String id, String category, boolean unanswerable,
            boolean vectorAny, boolean rerankAny, boolean vectorAll, boolean rerankAll,
            double vectorMrr, double rerankMrr) {
    }
}
