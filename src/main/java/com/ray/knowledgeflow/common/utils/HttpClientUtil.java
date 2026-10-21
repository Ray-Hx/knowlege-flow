package com.ray.knowledgeflow.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP客户端工具类
 * 基于Java 11 HttpClient实现
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
public class HttpClientUtil {

    private static final int MAX_RESPONSE_BYTES = 5 * 1024 * 1024;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @return 响应内容
     */
    public static String get(String url) throws Exception {
        return get(url, null);
    }

    /**
     * 发送GET请求（带headers）
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应内容
     */
    public static String get(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET();

        // 添加请求头
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        HttpRequest request = builder.build();

        log.debug("发送GET请求: {}", url);

        return sendForString(request);
    }

    /**
     * 发送POST请求
     *
     * @param url  请求URL
     * @param body 请求体
     * @return 响应内容
     */
    public static String post(String url, String body) throws Exception {
        return post(url, body, null);
    }

    /**
     * 发送POST请求（带headers）
     *
     * @param url     请求URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容
     */
    public static String post(String url, String body, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body));

        // 添加请求头
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        HttpRequest request = builder.build();

        log.debug("发送POST请求: {}", url);

        return sendForString(request);
    }

    private static String sendForString(HttpRequest request) throws Exception {
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP请求失败: " + response.statusCode());
            }
            byte[] bytes = body.readNBytes(MAX_RESPONSE_BYTES + 1);
            if (bytes.length > MAX_RESPONSE_BYTES) {
                throw new RuntimeException("HTTP响应超过 5MB 限制");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
