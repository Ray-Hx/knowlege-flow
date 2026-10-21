package com.ray.knowledgeflow.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void businessExceptionShouldSetMatchingHttpStatus() {
        var response = new GlobalExceptionHandler()
                .handleBusinessException(new BusinessException(404, "not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getCode());
    }
}
