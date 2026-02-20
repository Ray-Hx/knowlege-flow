package com.ray.knowledgeflow.module.graph.controller;

import com.ray.knowledgeflow.module.graph.entity.NoteNode;
import com.ray.knowledgeflow.module.graph.service.IGraphService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识图谱控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Tag(name = "知识图谱接口")
@RestController("graphControllerV1")
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final IGraphService graphService;

    @Operation(summary = "获取全量知识图谱")
    @GetMapping("/user")
    public List<NoteNode> getUserGraph() {
        Long userId = SecurityUtils.getCurrentUserId();
        return graphService.getGraph(userId);
    }

    @Operation(summary = "使用 AI 分析并建立自动关联")
    @PostMapping("/analyze")
    public Integer analyzeGraph() {
        Long userId = SecurityUtils.getCurrentUserId();
        return graphService.analyzeRelations(userId);
    }

    @Operation(summary = "使用 AI 分析并建立自动关联 (SSE流式进度)")
    @GetMapping("/analyze/stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter analyzeGraphStream() {
        Long userId = SecurityUtils.getCurrentUserId();

        // 设置超时时间为 10 分钟
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(
                10 * 60 * 1000L);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                graphService.analyzeRelations(userId, message -> {
                    try {
                        emitter.send(message);
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
