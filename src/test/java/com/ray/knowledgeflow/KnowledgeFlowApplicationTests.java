package com.ray.knowledgeflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.ray.knowledgeflow.module.search.repository.NoteRepository;
import com.ray.knowledgeflow.module.search.repository.ContentRepository;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeFlowApplicationTests {

    @MockBean
    private NoteRepository noteRepository;

    @MockBean
    private ContentRepository contentRepository;

    @Test
    void contextLoads() {
    }

}
