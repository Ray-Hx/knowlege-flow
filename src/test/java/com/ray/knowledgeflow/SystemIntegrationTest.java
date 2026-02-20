package com.ray.knowledgeflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.knowledgeflow.module.user.dto.LoginDto;
import com.ray.knowledgeflow.module.user.dto.RegisterDto;
import com.ray.knowledgeflow.module.user.service.CaptchaService;
import com.ray.knowledgeflow.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Map;
import co.elastic.clients.elasticsearch.ElasticsearchClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev") // Use local dev environment (Docker)
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false", // Disable RabbitMQ listeners
        "ZHIPUAI_API_KEY=dummy", // Dummy key for AiConfig
        "langchain4j.open-ai.api-key=dummy" // Dummy OpenAi key
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CaptchaService captchaService;

    // Mock AI & Search services to isolate system test from external deps
    @MockBean
    private dev.langchain4j.model.chat.ChatLanguageModel chatLanguageModel;

    @MockBean
    private dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    @MockBean
    private io.qdrant.client.QdrantClient qdrantClient;

    // Mock ISearchService to avoid complex logic in Controller test
    @MockBean
    private com.ray.knowledgeflow.module.search.service.ISearchService searchService;

    @MockBean
    private com.ray.knowledgeflow.module.search.repository.NoteRepository noteRepository;

    @MockBean
    private com.ray.knowledgeflow.module.search.repository.ContentRepository contentRepository;

    @MockBean
    private org.neo4j.driver.Driver neo4jDriver;

    private static String jwtToken;
    private static String testUsername;
    private static String testEmail;

    @BeforeEach
    void setUp() {
        // Always mock captcha to true
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);
        if (testUsername == null) {
            long timestamp = System.currentTimeMillis();
            testUsername = "u_" + timestamp; // Shorten to fit 20 char limit
            testEmail = "u_" + timestamp + "@example.com";
        }
    }

    @Test
    @Order(1)
    void testUserRegistration() throws Exception {
        RegisterDto registerDto = new RegisterDto();
        registerDto.setUsername(testUsername);
        registerDto.setPassword("password123");
        registerDto.setEmail(testEmail);
        registerDto.setCaptchaKey("key");
        registerDto.setCaptchaCode("1234"); // Mocked to pass

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andDo(print()) // Print result for debugging
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(2)
    void testUserLogin() throws Exception {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername(testUsername);
        loginDto.setPassword("password123");
        loginDto.setCaptchaKey("key");
        loginDto.setCaptchaCode("1234");

        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists()) // Token
                .andReturn();

        // Extract Token
        String response = result.getResponse().getContentAsString();
        Map map = objectMapper.readValue(response, Map.class);
        jwtToken = (String) map.get("data");
        System.out.println("Got Token: " + jwtToken);
    }

    @Test
    @Order(3)
    void testGetUserInfo() throws Exception {
        mockMvc.perform(get("/user/me")
                .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(testUsername));
    }

    @Autowired
    private com.ray.knowledgeflow.module.note.service.INoteService noteService;

    @Test
    @Order(4)
    void testSearchContent() throws Exception {
        // Stub search service
        when(searchService.searchContentHybrid(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList()));
        // Mock search result for hybrid search
        // Since we are using real services, but qdrant/es might be mocked or using dev
        // profile
        // Let's assume the Keyword search might return something if we had data, but
        // Vector is mocked.
        // Easiest is to test the endpoint connectivity and error handling,
        // or inject data if we can.
        // For now, let's verify parameters validation or empty result behavior.

        mockMvc.perform(get("/search/hybrid")
                .param("keyword", "test")
                .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    void testCreateNote() throws Exception {
        com.ray.knowledgeflow.module.note.dto.CreateNoteDto noteDto = new com.ray.knowledgeflow.module.note.dto.CreateNoteDto();
        noteDto.setTitle("Integration Test Note");
        noteDto.setContentMd("This is a note created during system integration test.");
        noteDto.setType("NOTE");
        noteDto.setStatus(1); // Draft

        mockMvc.perform(post("/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content(objectMapper.writeValueAsString(noteDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Integration Test Note"));
    }
}
