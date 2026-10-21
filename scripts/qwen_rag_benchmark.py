#!/usr/bin/env python3
"""Build a temporary Qwen-vector collection for a title-grounded RAG benchmark.

The existing content_chunks collection predates the Qwen switch and does not
store its embedding model. This script leaves it untouched and evaluates the
test user's points in a separate collection.
"""

import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
QDRANT = os.getenv("QDRANT_URL", "http://127.0.0.1:6333")
EMBED_URL = os.getenv(
    "QWEN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"
) + "/embeddings"
MODEL = os.getenv("QWEN_EMBEDDING_MODEL", "text-embedding-v4")
DIMENSIONS = int(os.getenv("QWEN_EMBEDDING_DIMENSIONS", "2048"))
USER_ID = 1
SOURCE_COLLECTION = "content_chunks"
TEMP_COLLECTION = "content_chunks_qwen_v4_2048"
RERANK_URL = os.getenv("RERANK_URL", "http://127.0.0.1:8081/rerank")


def json_request(url, method="GET", body=None, headers=None, timeout=90):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    req = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return json.loads(response.read())


def qdrant(path, method="GET", body=None):
    return json_request(
        QDRANT + path,
        method=method,
        body=body,
        headers={"Content-Type": "application/json"},
    )


def qwen_key():
    value = os.getenv("QWEN_API_KEY")
    if value:
        return value
    config = (ROOT / "src/main/resources/application.yml").read_text()
    match = re.search(r"^QWEN_API_KEY:\s*(\S+)\s*$", config, re.MULTILINE)
    if not match:
        raise RuntimeError("QWEN_API_KEY 未配置")
    return match.group(1)


def embed(texts):
    # DashScope text-embedding-v4 accepts at most 10 inputs per request.
    if len(texts) > 10:
        vectors = []
        for start in range(0, len(texts), 10):
            vectors.extend(embed(texts[start : start + 10]))
        return vectors
    payload = {"model": MODEL, "input": texts, "dimensions": DIMENSIONS}
    headers = {
        "Authorization": "Bearer " + qwen_key(),
        "Content-Type": "application/json",
    }
    for attempt in range(3):
        try:
            result = json_request(EMBED_URL, method="POST", body=payload, headers=headers)
            rows = sorted(result["data"], key=lambda row: row["index"])
            return [row["embedding"] for row in rows]
        except (urllib.error.HTTPError, urllib.error.URLError, KeyError) as exc:
            if attempt == 2:
                raise
            time.sleep(2 ** attempt)
    raise AssertionError("unreachable")


def scroll_points():
    points = []
    offset = None
    while True:
        body = {
            "limit": 100,
            "with_payload": True,
            "with_vector": False,
            "filter": {"must": [{"key": "userId", "match": {"value": USER_ID}}]},
        }
        if offset is not None:
            body["offset"] = offset
        result = qdrant(f"/collections/{SOURCE_COLLECTION}/points/scroll", "POST", body)
        batch = result["result"]["points"]
        points.extend(batch)
        offset = result["result"].get("next_page_offset")
        if not offset or not batch:
            return points


def doc_text(point):
    payload = point.get("payload", {})
    title = payload.get("title", "")
    content = payload.get("content", "")
    return f"标题：{title}\n{content}"


def ensure_temp_collection():
    try:
        qdrant(f"/collections/{TEMP_COLLECTION}")
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise
        qdrant(
            f"/collections/{TEMP_COLLECTION}",
            "PUT",
            {"vectors": {"size": DIMENSIONS, "distance": "Cosine"}},
        )


def temp_point_count():
    result = qdrant(
        f"/collections/{TEMP_COLLECTION}/points/count",
        "POST",
        {
            "exact": True,
            "filter": {"must": [{"key": "userId", "match": {"value": USER_ID}}]},
        },
    )
    return result["result"]["count"]


def upsert_qwen(points):
    ensure_temp_collection()
    for start in range(0, len(points), 10):
        batch = points[start : start + 10]
        vectors = embed([doc_text(point) for point in batch])
        qdrant(
            f"/collections/{TEMP_COLLECTION}/points?wait=true",
            "PUT",
            {
                "points": [
                    {"id": point["id"], "vector": vector, "payload": point["payload"]}
                    for point, vector in zip(batch, vectors)
                ]
            },
        )
        print(f"indexed {min(start + 10, len(points))}/{len(points)}", flush=True)


def search(vector):
    result = qdrant(
        f"/collections/{TEMP_COLLECTION}/points/search",
        "POST",
        {
            "vector": vector,
            "limit": 20,
            "with_payload": True,
            "score_threshold": 0.0,
            "filter": {"must": [{"key": "userId", "match": {"value": USER_ID}}]},
        },
    )
    return result["result"]


def rerank(query, documents):
    result = json_request(
        RERANK_URL,
        method="POST",
        body={"query": query, "documents": documents, "top_n": 5},
        headers={"Content-Type": "application/json"},
        timeout=60,
    )
    return [row["index"] for row in result["results"][:5]]


def norm(value):
    return re.sub(r"\s+", "", value or "").casefold()


def unique(values):
    seen = set()
    result = []
    for value in values:
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def main():
    cases_path = Path(sys.argv[1] if len(sys.argv) > 1 else ROOT / "scripts/rag_corpus_eval_cases.json")
    cases = json.loads(cases_path.read_text())
    points = scroll_points()
    print(f"source_points_user_{USER_ID}={len(points)}", flush=True)
    ensure_temp_collection()
    if temp_point_count() == len(points):
        print(f"reuse {TEMP_COLLECTION} ({len(points)} points)", flush=True)
    else:
        upsert_qwen(points)

    query_vectors = embed([case["query"] for case in cases])
    rows = []
    for case, vector in zip(cases, query_vectors):
        candidates = search(vector)
        texts = [doc_text(point) for point in candidates]
        vector_titles = [norm(point.get("payload", {}).get("title", "")) for point in candidates[:5]]
        rerank_indices = rerank(case["query"], texts)
        rerank_titles = [norm(candidates[index].get("payload", {}).get("title", "")) for index in rerank_indices]
        gold = {norm(title) for title in case.get("gold_titles", [])}
        vector_rank = next((i + 1 for i, title in enumerate(vector_titles) if title in gold), 0)
        rerank_rank = next((i + 1 for i, title in enumerate(rerank_titles) if title in gold), 0)
        # Compare unique documents over the same top-5 window. The vector
        # search returns 20 chunks, but reranker only returns 5; using all 20
        # here would inflate vector recall and make the comparison unfair.
        vector_unique_titles = unique(vector_titles)
        rerank_unique_titles = unique(rerank_titles)
        vector_unique_rank = next((i + 1 for i, title in enumerate(vector_unique_titles) if title in gold), 0)
        rerank_unique_rank = next((i + 1 for i, title in enumerate(rerank_unique_titles) if title in gold), 0)
        vector_coverage = len(gold.intersection(vector_unique_titles)) / len(gold) if gold else 0.0
        rerank_coverage = len(gold.intersection(rerank_unique_titles)) / len(gold) if gold else 0.0
        rows.append({
            "id": case["id"],
            "category": case.get("category", ""),
            "vector_hit": bool(vector_rank),
            "rerank_hit": bool(rerank_rank),
            "vector_mrr": 1 / vector_rank if vector_rank else 0,
            "rerank_mrr": 1 / rerank_rank if rerank_rank else 0,
            "vector_unique_hit": bool(vector_unique_rank),
            "rerank_unique_hit": bool(rerank_unique_rank),
            "vector_unique_hit_at_1": vector_unique_rank == 1,
            "rerank_unique_hit_at_1": rerank_unique_rank == 1,
            "vector_unique_mrr": 1 / vector_unique_rank if vector_unique_rank else 0,
            "rerank_unique_mrr": 1 / rerank_unique_rank if rerank_unique_rank else 0,
            "vector_all_gold": bool(gold) and vector_coverage == 1.0,
            "rerank_all_gold": bool(gold) and rerank_coverage == 1.0,
            "vector_coverage": vector_coverage,
            "rerank_coverage": rerank_coverage,
            "vector_top1_score": candidates[0].get("score", 0.0) if candidates else 0.0,
            "title_in_query": any(norm(title) in norm(case["query"]) for title in case.get("gold_titles", [])),
        })
        print(f"{case['id']} vector={vector_titles} rerank={rerank_titles}", flush=True)

    answerable = [row for row, case in zip(rows, cases) if case.get("gold_titles")]
    unanswerable = [row for row, case in zip(rows, cases) if not case.get("gold_titles")]

    def category_metrics(category):
        subset = [row for row, case in zip(rows, cases) if case.get("category") == category and case.get("gold_titles")]
        if not subset:
            return {"cases": 0}
        return {
            "cases": len(subset),
            "vector_unique_hit_at_1": sum(row["vector_unique_hit_at_1"] for row in subset),
            "rerank_unique_hit_at_1": sum(row["rerank_unique_hit_at_1"] for row in subset),
            "vector_unique_hit_at_5": sum(row["vector_unique_hit"] for row in subset),
            "rerank_unique_hit_at_5": sum(row["rerank_unique_hit"] for row in subset),
            "vector_all_gold_at_5": sum(row["vector_all_gold"] for row in subset),
            "rerank_all_gold_at_5": sum(row["rerank_all_gold"] for row in subset),
            "vector_coverage_at_5": round(sum(row["vector_coverage"] for row in subset) / len(subset), 3),
            "rerank_coverage_at_5": round(sum(row["rerank_coverage"] for row in subset) / len(subset), 3),
            "vector_mrr": round(sum(row["vector_unique_mrr"] for row in subset) / len(subset), 3),
            "rerank_mrr": round(sum(row["rerank_unique_mrr"] for row in subset) / len(subset), 3),
        }
    summary = {
        "collection": TEMP_COLLECTION,
        "model": MODEL,
        "dimensions": DIMENSIONS,
        "cases": len(cases),
        "answerable_cases": len(answerable),
        "vector_hit_at_5": sum(row["vector_hit"] for row in answerable),
        "rerank_hit_at_5": sum(row["rerank_hit"] for row in answerable),
        "vector_mrr_at_5": round(sum(row["vector_mrr"] for row in answerable) / len(answerable), 3),
        "rerank_mrr_at_5": round(sum(row["rerank_mrr"] for row in answerable) / len(answerable), 3),
        "unique_title_vector_hit_at_5": sum(row["vector_unique_hit"] for row in answerable),
        "unique_title_rerank_hit_at_5": sum(row["rerank_unique_hit"] for row in answerable),
        "unique_title_vector_hit_at_1": sum(row["vector_unique_hit_at_1"] for row in answerable),
        "unique_title_rerank_hit_at_1": sum(row["rerank_unique_hit_at_1"] for row in answerable),
        "unique_title_vector_mrr": round(sum(row["vector_unique_mrr"] for row in answerable) / len(answerable), 3),
        "unique_title_rerank_mrr": round(sum(row["rerank_unique_mrr"] for row in answerable) / len(answerable), 3),
        "gold_title_explicitly_in_query": sum(row["title_in_query"] for row in answerable),
        "all_gold_vector_at_5": sum(row["vector_all_gold"] for row in answerable),
        "all_gold_rerank_at_5": sum(row["rerank_all_gold"] for row in answerable),
        "mean_gold_coverage_vector_at_5": round(sum(row["vector_coverage"] for row in answerable) / len(answerable), 3),
        "mean_gold_coverage_rerank_at_5": round(sum(row["rerank_coverage"] for row in answerable) / len(answerable), 3),
        "by_category": {
            category: category_metrics(category)
            for category in sorted({case.get("category", "") for case in cases})
        },
        "unanswerable_cases": len(unanswerable),
        "unanswerable_mean_vector_top1_score": round(sum(row["vector_top1_score"] for row in unanswerable) / len(unanswerable), 4) if unanswerable else None,
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
