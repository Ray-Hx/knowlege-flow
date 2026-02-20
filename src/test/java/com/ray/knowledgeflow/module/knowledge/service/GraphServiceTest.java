package com.ray.knowledgeflow.module.knowledge.service;

import com.ray.knowledgeflow.module.knowledge.entity.graph.NoteNode;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphServiceTest {

    @Mock
    private Neo4jTemplate neo4jTemplate;

    @Mock
    private LinkParserService linkParserService;

    private GraphService graphService;

    @BeforeEach
    void setUp() {
        graphService = new GraphService(neo4jTemplate, linkParserService);
    }

    @Test
    void syncNoteToGraph_ShouldCreateRelationships_WhenLinksExist() {
        // Arrange
        NoteEntry note = new NoteEntry();
        note.setId(1L);
        note.setTitle("Source Note");
        note.setContentMd("Links to [[Target Note]]");
        note.setSummary("Summary");

        when(linkParserService.extractWikiLinks(anyString())).thenReturn(Set.of("Target Note"));

        NoteNode targetNode = new NoteNode();
        targetNode.setNoteId(2L);
        targetNode.setTitle("Target Note");

        // Mock finding the target note by title
        // 注意: findOne 的第二个参数是 Map filter
        doReturn(Optional.of(targetNode)).when(neo4jTemplate).findOne(anyString(), any(Map.class), eq(NoteNode.class));

        // Act
        graphService.syncNoteToGraph(note);

        // Assert
        ArgumentCaptor<NoteNode> captor = ArgumentCaptor.forClass(NoteNode.class);
        verify(neo4jTemplate).save(captor.capture());

        NoteNode savedNode = captor.getValue();
        assertEquals(1L, savedNode.getNoteId());
        assertEquals("Source Note", savedNode.getTitle());
        assertEquals(1, savedNode.getReferences().size());
        assertEquals(2L, savedNode.getReferences().iterator().next().getNoteId());
    }

    @Test
    void getGraphData_ShouldReturnNodesAndLinks() {
        // Arrange
        NoteNode n1 = new NoteNode();
        n1.setNoteId(1L);
        n1.setTitle("Note 1");

        NoteNode n2 = new NoteNode();
        n2.setNoteId(2L);
        n2.setTitle("Note 2");

        n1.setReferences(java.util.Set.of(n2));

        doReturn(java.util.List.of(n1, n2)).when(neo4jTemplate).findAll(NoteNode.class);

        // Act
        java.util.Map<String, Object> graphData = graphService.getGraphData();

        // Assert
        java.util.List<?> nodes = (java.util.List<?>) graphData.get("nodes");
        java.util.List<?> links = (java.util.List<?>) graphData.get("links");

        assertEquals(2, nodes.size());
        assertEquals(1, links.size());
    }
}
