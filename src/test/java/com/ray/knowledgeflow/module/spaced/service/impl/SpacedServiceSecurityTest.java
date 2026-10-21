package com.ray.knowledgeflow.module.spaced.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.ai.service.IAiService;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.spaced.mapper.NoteReviewQuestionMapper;
import com.ray.knowledgeflow.module.spaced.mapper.SpacedLogMapper;
import com.ray.knowledgeflow.module.spaced.mapper.SpacedReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpacedServiceSecurityTest {

    @Mock private SpacedReviewMapper reviewMapper;
    @Mock private NoteReviewQuestionMapper questionMapper;
    @Mock private NoteEntryMapper noteMapper;
    @Mock private SpacedLogMapper spacedLogMapper;
    @Mock private IAiService aiService;

    private SpacedServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SpacedServiceImpl(reviewMapper, questionMapper, noteMapper, spacedLogMapper, aiService);
    }

    @Test
    void generateQuestions_ShouldRejectNoteNotOwnedByCurrentUser() {
        when(noteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.generateQuestions(7L, 99L));

        verify(questionMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(aiService, never()).generateReviewQuestions(any(), any());
    }
}
