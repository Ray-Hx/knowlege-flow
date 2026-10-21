package com.ray.knowledgeflow.module.knowledge.service;

import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteLink;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteLinkMapper;
import com.ray.knowledgeflow.module.note.service.NoteLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

/**
 * 旧 LinkParserService 迁移到 NoteLinkService 后的兼容语法回归测试。
 */
@ExtendWith(MockitoExtension.class)
class LinkParserServiceTest {

    @Mock private NoteLinkMapper noteLinkMapper;
    @Mock private NoteEntryMapper noteEntryMapper;

    @Test
    void legacyWikiLinks_ShouldResolveUniqueTitlesAndAliases() {
        when(noteEntryMapper.selectList(any()))
                .thenReturn(List.of(note(1L, "Note A")))
                .thenReturn(List.of(note(2L, "Note B")))
                .thenReturn(List.of(note(3L, "Note C")));

        NoteEntry source = note(9L, "Source");
        source.setContentMd("Check [[Note A]], [[Note B|Alias]] and [[Note C]]");
        NoteLinkService service = new NoteLinkService(noteLinkMapper, noteEntryMapper);
        service.syncExplicitLinks(source);

        verify(noteLinkMapper, atLeastOnce()).delete(any());
        ArgumentCaptor<NoteLink> captor = ArgumentCaptor.forClass(NoteLink.class);
        verify(noteLinkMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertEquals(List.of(1L, 2L, 3L), captor.getAllValues().stream()
                .map(NoteLink::getTargetNoteId)
                .toList());
    }

    private static NoteEntry note(Long id, String title) {
        NoteEntry note = new NoteEntry();
        note.setId(id);
        note.setUserId(7L);
        note.setTitle(title);
        return note;
    }
}
