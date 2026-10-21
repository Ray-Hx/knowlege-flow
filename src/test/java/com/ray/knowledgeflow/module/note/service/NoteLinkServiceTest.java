package com.ray.knowledgeflow.module.note.service;

import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteLink;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteLinkMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteLinkServiceTest {

    @Mock
    private NoteLinkMapper noteLinkMapper;
    @Mock
    private NoteEntryMapper noteEntryMapper;

    @Test
    void stableIdLinkSurvivesTitleChangesAndRejectsForeignNotes() {
        NoteEntry owned = note(2L, 7L, "已经改名");
        NoteEntry foreign = note(3L, 8L, "别人的笔记");
        when(noteEntryMapper.selectById(2L)).thenReturn(owned);
        when(noteEntryMapper.selectById(3L)).thenReturn(foreign);

        NoteLinkService service = new NoteLinkService(noteLinkMapper, noteEntryMapper);
        LinkedHashSet<Long> ids = service.parseTargetIds(
                7L, "[[note:2|旧标题]] 和 [[note:3|别人的笔记]]");

        assertEquals(List.of(2L), ids.stream().toList());
    }

    @Test
    void legacyAliasIsResolvedOnlyWhenTitleIsUnique() {
        when(noteEntryMapper.selectList(any()))
                .thenReturn(List.of(note(2L, 7L, "目标笔记")))
                .thenReturn(List.of(note(3L, 7L, "重复"), note(4L, 7L, "重复")));

        NoteLinkService service = new NoteLinkService(noteLinkMapper, noteEntryMapper);
        LinkedHashSet<Long> ids = service.parseTargetIds(
                7L, "[[目标笔记|显示别名]] 和 [[重复]]");

        assertEquals(List.of(2L), ids.stream().toList());
    }

    @Test
    void syncExplicitLinksAtomicallyReplacesOutgoingRelations() {
        NoteEntry source = note(1L, 7L, "来源");
        source.setContentMd("[[note:2|目标]] [[note:2|重复引用]]");
        when(noteEntryMapper.selectById(2L)).thenReturn(note(2L, 7L, "目标"));

        NoteLinkService service = new NoteLinkService(noteLinkMapper, noteEntryMapper);
        service.syncExplicitLinks(source);

        verify(noteLinkMapper, atLeastOnce()).delete(any());
        ArgumentCaptor<NoteLink> captor = ArgumentCaptor.forClass(NoteLink.class);
        verify(noteLinkMapper).insert(captor.capture());
        NoteLink inserted = captor.getValue();
        assertEquals(1L, inserted.getSourceNoteId());
        assertEquals(2L, inserted.getTargetNoteId());
        assertEquals(NoteLink.TYPE_EXPLICIT, inserted.getLinkType());
    }

    private static NoteEntry note(Long id, Long userId, String title) {
        NoteEntry note = new NoteEntry();
        note.setId(id);
        note.setUserId(userId);
        note.setTitle(title);
        note.setIsPublic(1);
        note.setStatus(2);
        return note;
    }
}
