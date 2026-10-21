package com.ray.knowledgeflow.module.search.service.impl;

import com.ray.knowledgeflow.module.ai.service.Reranker;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.note.entity.NoteFolder;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
import com.ray.knowledgeflow.module.search.entity.ContentDocument;
import com.ray.knowledgeflow.module.search.entity.NoteDocument;
import com.ray.knowledgeflow.module.search.repository.ContentRepository;
import com.ray.knowledgeflow.module.search.repository.NoteRepository;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceIndexingTest {

    @Mock private NoteRepository noteRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private NoteFolderMapper noteFolderMapper;
    @Mock private ElasticsearchOperations elasticsearchOperations;
    @Mock private EmbeddingService embeddingService;
    @Mock private QdrantClient qdrantClient;
    @Mock private ContentItemMapper contentItemMapper;
    @Mock private NoteEntryMapper noteMapper;
    @Mock private Reranker reranker;
    @Mock private java.util.concurrent.Executor searchTaskExecutor;

    @InjectMocks private SearchServiceImpl searchService;

    @Test
    void syncContent_ShouldMapMysqlContentTextToElasticsearchContent() {
        ContentItem item = new ContentItem();
        item.setId(11L);
        item.setContentText("indexed body");
        item.setTags("[\"java\",\"rag\"]");

        searchService.syncContent(item);

        ArgumentCaptor<ContentDocument> captor = ArgumentCaptor.forClass(ContentDocument.class);
        verify(contentRepository).save(captor.capture());
        assertEquals("indexed body", captor.getValue().getContent());
        assertEquals(List.of("java", "rag"), captor.getValue().getTags());
    }

    @Test
    void searchNotes_ShouldLoadFolderNamesInOneBatch() {
        NoteDocument first = noteDocument(1L, 20L);
        NoteDocument second = noteDocument(2L, 20L);
        SearchHit<NoteDocument> firstHit = new SearchHit<>(
                null, "", "", 1.0f, null, null, null, null, null, null, first);
        SearchHit<NoteDocument> secondHit = new SearchHit<>(
                null, "", "", 0.9f, null, null, null, null, null, null, second);

        @SuppressWarnings("unchecked")
        SearchHits<NoteDocument> hits = mock(SearchHits.class);
        when(hits.stream()).thenAnswer(invocation -> List.of(firstHit, secondHit).stream());
        when(hits.getTotalHits()).thenReturn(2L);
        when(elasticsearchOperations.search(any(Query.class), eq(NoteDocument.class))).thenReturn(hits);

        NoteFolder folder = new NoteFolder();
        folder.setId(20L);
        folder.setUserId(7L);
        folder.setName("Architecture");
        when(noteFolderMapper.selectList(any())).thenReturn(List.of(folder));

        var result = searchService.searchNotes("java", 7L, 0, 10);

        assertEquals(List.of("Architecture", "Architecture"), result.getContent().stream()
                .map(dto -> dto.getFolderName())
                .toList());
        verify(noteFolderMapper).selectList(any());
        verify(noteFolderMapper, never()).selectById(any());
    }

    @Test
    void globalSearch_ShouldKeepRelevanceAheadOfRecency() {
        NoteDocument recentButWeak = new NoteDocument();
        recentButWeak.setId(1L);
        recentButWeak.setTitle("recent");
        recentButWeak.setUpdatedTime(LocalDateTime.of(2026, 8, 31, 12, 0));
        SearchHit<NoteDocument> noteHit = new SearchHit<>(
                null, "", "", 0.5f, null, null, null, null, null, null, recentButWeak);

        ContentDocument oldButRelevant = new ContentDocument();
        oldButRelevant.setId(2L);
        oldButRelevant.setTitle("algorithm");
        oldButRelevant.setCreatedTime(LocalDateTime.of(2024, 1, 1, 12, 0));
        SearchHit<ContentDocument> contentHit = new SearchHit<>(
                null, "", "", 5.0f, null, null, null, null, null, null, oldButRelevant);

        @SuppressWarnings("unchecked")
        SearchHits<NoteDocument> noteHits = mock(SearchHits.class);
        when(noteHits.iterator()).thenAnswer(invocation -> List.of(noteHit).iterator());
        @SuppressWarnings("unchecked")
        SearchHits<ContentDocument> contentHits = mock(SearchHits.class);
        when(contentHits.iterator()).thenAnswer(invocation -> List.of(contentHit).iterator());
        when(elasticsearchOperations.search(any(Query.class), eq(NoteDocument.class))).thenReturn(noteHits);
        when(elasticsearchOperations.search(any(Query.class), eq(ContentDocument.class))).thenReturn(contentHits);

        var result = searchService.globalSearch("algorithm", 7L);

        assertEquals(List.of("2", "1"), result.stream().map(dto -> dto.getId()).toList());
    }

    private static NoteDocument noteDocument(Long id, Long folderId) {
        NoteDocument document = new NoteDocument();
        document.setId(id);
        document.setFolderId(folderId);
        document.setTitle("Note " + id);
        return document;
    }
}
