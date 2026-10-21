package com.ray.knowledgeflow.module.knowledge.service;

import com.ray.knowledgeflow.module.graph.entity.NoteNode;
import com.ray.knowledgeflow.module.graph.repository.GraphNoteRepository;
import com.ray.knowledgeflow.module.graph.service.impl.GraphServiceImpl;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.service.NoteLinkService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.ray.knowledgeflow.common.exception.BusinessException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 当前图谱实现的回归测试。原测试依赖已删除的 knowledge.GraphService，导致整个测试集无法编译。
 */
@ExtendWith(MockitoExtension.class)
class GraphServiceTest {

    @Mock private GraphNoteRepository graphRepository;
    @Mock private NoteEntryMapper noteEntryMapper;
    @Mock private NoteLinkService noteLinkService;
    @Mock private ChatLanguageModel chatModel;
    @Mock private TransactionTemplate neo4jTransactionTemplate;

    private GraphServiceImpl graphService;

    @BeforeEach
    void setUp() {
        graphService = new GraphServiceImpl(
                graphRepository, noteEntryMapper, noteLinkService, chatModel, neo4jTransactionTemplate);
    }

    @Test
    void syncNote_ShouldReplaceOutgoingLinksWithOwnedTargets() {
        NoteEntry source = note(1L, 7L, "Source Note");
        NoteEntry target = note(2L, 7L, "Target Note");
        when(graphRepository.findById(1L)).thenReturn(Optional.empty());
        when(graphRepository.findById(2L)).thenReturn(Optional.empty());
        when(noteEntryMapper.selectById(2L)).thenReturn(target);

        graphService.syncNote(source, List.of(2L));

        verify(graphRepository).deleteOutgoingLinks(1L);
        ArgumentCaptor<NoteNode> captor = ArgumentCaptor.forClass(NoteNode.class);
        verify(graphRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        NoteNode savedSource = captor.getAllValues().stream()
                .filter(node -> Long.valueOf(1L).equals(node.getId()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertEquals(1, savedSource.getLinks().size());
        assertEquals(2L, savedSource.getLinks().iterator().next().getId());
    }

    @Test
    void getGraph_ShouldMergeNeo4jRelationsOntoMysqlOwnedNodes() {
        NoteEntry source = note(1L, 7L, "Note 1");
        NoteEntry target = note(2L, 7L, "Note 2");
        when(noteEntryMapper.selectList(any())).thenReturn(List.of(source, target));

        NoteNode graphSource = new NoteNode(1L, "stale title", 7L);
        graphSource.addLink(new NoteNode(2L, "Note 2", 7L));
        when(graphRepository.findGraphByUserId(7L)).thenReturn(List.of(graphSource));

        List<NoteNode> graph = graphService.getGraph(7L);

        assertEquals(2, graph.size());
        NoteNode mergedSource = graph.stream().filter(node -> node.getId().equals(1L)).findFirst().orElseThrow();
        assertEquals("Note 1", mergedSource.getTitle());
        assertEquals(1, mergedSource.getLinks().size());
    }

    @Test
    void analyzeRelations_ShouldPreserveExistingRelationsWhenModelProtocolIsInvalid() {
        NoteEntry first = note(1L, 7L, "First");
        NoteEntry second = note(2L, 7L, "Second");
        when(noteEntryMapper.selectList(any())).thenReturn(List.of(first, second));
        when(chatModel.generate(any(String.class))).thenReturn("我认为这两篇很相关，但不按协议返回");

        assertThrows(BusinessException.class,
                () -> graphService.analyzeRelations(7L, ignored -> { }));

        verify(graphRepository, org.mockito.Mockito.never()).deleteRelatedByUserId(any());
        verify(neo4jTransactionTemplate, org.mockito.Mockito.never()).execute(any());
    }

    private static NoteEntry note(Long id, Long userId, String title) {
        NoteEntry note = new NoteEntry();
        note.setId(id);
        note.setUserId(userId);
        note.setTitle(title);
        return note;
    }
}
