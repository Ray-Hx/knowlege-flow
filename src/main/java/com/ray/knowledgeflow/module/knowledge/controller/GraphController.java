package com.ray.knowledgeflow.module.knowledge.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.knowledge.service.GraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 知识图谱控制器
 *
 * @author Ray
 * @since 2024-01-21
 */
@Tag(name = "知识图谱接口")
@RestController
@RequestMapping("/knowledge/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    @Operation(summary = "获取全量图谱数据 (D3格式)")
    @GetMapping
    public Result<Map<String, Object>> getGraph() {
        return Result.success(graphService.getGraphData());
    }
}
