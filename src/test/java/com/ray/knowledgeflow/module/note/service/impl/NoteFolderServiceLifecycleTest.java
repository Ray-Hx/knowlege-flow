package com.ray.knowledgeflow.module.note.service.impl;

import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteFolder;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
import com.ray.knowledgeflow.module.note.service.INoteService;
import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteFolderServiceLifecycleTest {

    @Test
    void deletingFolderShouldUseCompleteNoteDeletionLifecycle() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), NoteFolder.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), NoteEntry.class);
        NoteFolderMapper folderMapper = mock(NoteFolderMapper.class);
        NoteEntryMapper noteMapper = mock(NoteEntryMapper.class);
        INoteService noteService = mock(INoteService.class);
        NoteFolderServiceImpl service = new NoteFolderServiceImpl(folderMapper, noteMapper, noteService);

        NoteFolder root = folder(10L, 0L, 7L);
        NoteFolder child = folder(11L, 10L, 7L);
        when(folderMapper.selectById(10L)).thenReturn(root);
        when(folderMapper.selectList(any())).thenReturn(List.of(root, child));

        NoteEntry first = new NoteEntry();
        first.setId(100L);
        NoteEntry second = new NoteEntry();
        second.setId(101L);
        when(noteMapper.selectList(any())).thenReturn(List.of(first, second));

        service.deleteFolder(7L, 10L);

        verify(noteService).deleteNote(7L, 100L);
        verify(noteService).deleteNote(7L, 101L);
        var order = inOrder(folderMapper);
        order.verify(folderMapper).deleteById(11L);
        order.verify(folderMapper).deleteById(10L);
    }

    private static NoteFolder folder(Long id, Long parentId, Long userId) {
        NoteFolder folder = new NoteFolder();
        folder.setId(id);
        folder.setParentId(parentId);
        folder.setUserId(userId);
        return folder;
    }
}
