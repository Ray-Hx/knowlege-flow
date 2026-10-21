package com.ray.knowledgeflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.ray.knowledgeflow.module.search.repository.NoteRepository;
import com.ray.knowledgeflow.module.search.repository.ContentRepository;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeFlowApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private NoteRepository noteRepository;

    @MockBean
    private ContentRepository contentRepository;

    @Test
    void contextLoads() {
        assertInstanceOf(JdbcTransactionManager.class,
                applicationContext.getBean("transactionManager"));
        assertInstanceOf(Neo4jTransactionManager.class,
                applicationContext.getBean("neo4jTransactionManager"));
    }

}
