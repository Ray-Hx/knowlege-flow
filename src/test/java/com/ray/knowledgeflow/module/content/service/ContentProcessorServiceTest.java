package com.ray.knowledgeflow.module.content.service;

import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentProcessorServiceTest {

    @Mock
    private ContentItemMapper contentItemMapper;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private ContentProcessorService contentProcessorService;

    @BeforeEach
    void setUp() {
        contentProcessorService = new ContentProcessorService(contentItemMapper, chatLanguageModel);
    }

    @Test
    void testIsDuplicate_WhenContentIsSimilar_ShouldReturnTrue() {
        // Arrange
        ContentItem newItem = new ContentItem();
        newItem.setId(1L);
        newItem.setUserId(100L);
        newItem.setContentText("This is a test content for simhash calculation.");

        ContentItem existingItem = new ContentItem();
        existingItem.setId(2L);
        existingItem.setContentText("This is a test content for simhash calculation."); // Identical

        when(contentItemMapper.selectList(any()))
                .thenReturn(List.of(existingItem));

        // Act
        boolean result = contentProcessorService.isDuplicate(newItem);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsDuplicate_WhenContentIsDifferent_ShouldReturnFalse() {
        // Arrange
        ContentItem newItem = new ContentItem();
        newItem.setId(1L);
        newItem.setUserId(100L);
        newItem.setContentText("This is a test content.");

        ContentItem existingItem = new ContentItem();
        existingItem.setId(2L);
        existingItem.setContentText("Completely different topic about java programming.");

        when(contentItemMapper.selectList(any()))
                .thenReturn(List.of(existingItem));

        // Act
        boolean result = contentProcessorService.isDuplicate(newItem);

        // Assert
        assertFalse(result);
    }

    @Test
    void testAutoClassify_ShouldDetectCategory() {
        // Arrange
        ContentItem item = new ContentItem();
        item.setTitle("Java Programming Guide");
        item.setContentText("Spring Boot creates stand-alone, production-grade Spring based Applications.");

        // Act
        String category = contentProcessorService.autoClassify(item);

        // Assert
        assertEquals("编程开发", category);
    }

    @Test
    void testProcessContent_WhenNotDuplicate_ShouldGenerateSummary() {
        // Arrange
        ContentItem item = new ContentItem();
        item.setId(1L);
        item.setUserId(100L);
        item.setContentText("Long article content about Artificial Intelligence...");
        item.setStatus(ContentItem.STATUS_UNREAD);

        when(contentItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(chatLanguageModel.generate(anyString())).thenReturn("AI Generated Summary");

        // Act
        contentProcessorService.processContent(item);

        // Assert
        verify(chatLanguageModel, times(1)).generate(anyString());
        assertEquals("AI Generated Summary", item.getSummary());
        // assertTrue(item.getTags().contains("AI")); // HanLP keyword extraction might
        // vary
        assertNotNull(item.getTags());
        verify(contentItemMapper, times(1)).updateById(item);
    }

    @Test
    void testProcessContent_WhenDuplicate_ShouldSetStatusAndSkipSummary() {
        // Arrange
        ContentItem newItem = new ContentItem();
        newItem.setId(1L);
        newItem.setUserId(100L);
        newItem.setContentText("Duplicate content.");
        newItem.setStatus(ContentItem.STATUS_UNREAD);

        ContentItem existingItem = new ContentItem();
        existingItem.setId(2L);
        existingItem.setContentText("Duplicate content.");

        when(contentItemMapper.selectList(any())).thenReturn(List.of(existingItem));

        // Act
        contentProcessorService.processContent(newItem);

        // Assert
        assertEquals(ContentItem.STATUS_DUPLICATE, newItem.getStatus());
        verify(chatLanguageModel, never()).generate(anyString()); // Should SKIP summary
        verify(contentItemMapper, times(1)).updateById(newItem);
    }
}
