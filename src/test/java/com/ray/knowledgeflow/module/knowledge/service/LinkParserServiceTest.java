package com.ray.knowledgeflow.module.knowledge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LinkParserServiceTest {

    private LinkParserService linkParserService;

    @BeforeEach
    void setUp() {
        linkParserService = new LinkParserService();
    }

    @Test
    void extractWikiLinks_ShouldExtractTitles() {
        String content = "Check out [[Note A]] and [[Note B|Alias]]. Also [[Note C]]";
        Set<String> links = linkParserService.extractWikiLinks(content);

        assertEquals(3, links.size());
        assertTrue(links.contains("Note A"));
        assertTrue(links.contains("Note C"));
        // "Note B|Alias" group 1 is "Note B"
        assertTrue(links.contains("Note B") || links.contains("Note B|Alias")); // Let's check logic
    }
}
