package com.ray.knowledgeflow.module.note.service.impl;

import com.ray.knowledgeflow.common.utils.MinioUtil;
import com.ray.knowledgeflow.module.ai.message.AiProcessProducer;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.mapper.NoteAttachmentMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteVersionMapper;
import com.ray.knowledgeflow.module.note.service.NoteEngagementService;
import com.ray.knowledgeflow.module.note.service.NoteLinkService;
import com.ray.knowledgeflow.module.search.message.IndexUpdateProducer;
import com.ray.knowledgeflow.module.search.service.ISearchService;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import com.ray.knowledgeflow.module.user.mapper.UserProfileMapper;
import com.ray.knowledgeflow.module.user.service.IUserRelationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公开笔记详情的可见性回归测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoteServicePublicDetailTest {

    @Mock
    private NoteEntryMapper noteEntryMapper;
    @Mock
    private NoteFolderMapper folderMapper;
    @Mock
    private NoteVersionMapper versionMapper;
    @Mock
    private NoteAttachmentMapper attachmentMapper;
    @Mock
    private ContentItemMapper contentItemMapper;
    @Mock
    private ContentSourceMapper contentSourceMapper;
    @Mock
    private MinioUtil minioUtil;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AiProcessProducer aiProcessProducer;
    @Mock
    private IndexUpdateProducer indexUpdateProducer;
    @Mock
    private NoteEngagementService engagementService;
    @Mock
    private ISearchService searchService;
    @Mock
    private IUserRelationService userRelationService;
    @Mock
    private NoteLinkService noteLinkService;

    @InjectMocks
    private NoteServiceImpl noteService;

    /**
     * 该接口对匿名访客开放，双向链接查询必须带 is_public 过滤，
     * 否则作者私有笔记的标题与摘要会随公开笔记一并泄露。
     */
    @Test
    void getPublicNoteDetail_ShouldOnlyExposePublicLinkedNotes() {
        NoteEntry publicNote = new NoteEntry();
        publicNote.setId(1L);
        publicNote.setUserId(9L);
        publicNote.setTitle("Java 并发笔记");
        publicNote.setContentMd("参见 [[秋招投递记录]]");
        publicNote.setIsPublic(1);
        publicNote.setStatus(2);

        when(noteEntryMapper.selectById(1L)).thenReturn(publicNote);
        noteService.getPublicNoteDetail(1L);

        verify(noteLinkService).getOutgoingLinks(9L, 1L, true);
        verify(noteLinkService).getIncomingLinks(9L, 1L, true);
    }
}
