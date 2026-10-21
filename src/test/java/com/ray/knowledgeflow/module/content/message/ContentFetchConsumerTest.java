package com.ray.knowledgeflow.module.content.message;

import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.content.service.IArXivFetchService;
import com.ray.knowledgeflow.module.content.service.IGitHubFetchService;
import com.ray.knowledgeflow.module.content.service.IRssFetchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ContentFetchConsumerTest {

    @Mock private IRssFetchService rssFetchService;
    @Mock private IGitHubFetchService gitHubFetchService;
    @Mock private IArXivFetchService arXivFetchService;
    @Mock private ContentSourceMapper contentSourceMapper;

    private ContentFetchConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ContentFetchConsumer(
                rssFetchService, gitHubFetchService, arXivFetchService, contentSourceMapper);
    }

    @Test
    void shouldPropagateFetchFailureSoRabbitRetryCanRun() {
        doThrow(new IllegalStateException("network down"))
                .when(rssFetchService).fetchFromSource(3L, 7L);

        assertThrows(IllegalStateException.class,
                () -> consumer.handleFetchTask(new ContentFetchMessage("RSS", 3L, 7L, null)));
        verifyNoInteractions(contentSourceMapper);
    }

    @Test
    void shouldUpdateLastSyncOnlyAfterSuccessfulFetch() {
        consumer.handleFetchTask(new ContentFetchMessage("ARXIV", 3L, 7L, null));

        verify(arXivFetchService).fetchFromSource(3L, 7L);
        verify(contentSourceMapper).update(any(), any());
    }

    @Test
    void shouldRejectMalformedMessage() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.handleFetchTask(new ContentFetchMessage(null, 3L, 7L, null)));
    }
}
