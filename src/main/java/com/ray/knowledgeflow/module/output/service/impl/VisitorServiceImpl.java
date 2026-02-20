package com.ray.knowledgeflow.module.output.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.output.dto.VisitorStatsDto;
import com.ray.knowledgeflow.module.output.entity.VisitorLog;
import com.ray.knowledgeflow.module.output.mapper.VisitorLogMapper;
import com.ray.knowledgeflow.module.output.service.IVisitorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 访客服务实现
 *
 * @author Ray
 * @since 2024-01-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorServiceImpl implements IVisitorService {

    private final VisitorLogMapper visitorLogMapper;

    @Override
    public void recordVisit(Long userId, String resourceType, Long resourceId, HttpServletRequest request) {
        VisitorLog log = new VisitorLog();
        log.setUserId(userId);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setVisitorIp(getClientIp(request));
        log.setVisitorUa(request.getHeader("User-Agent"));
        log.setReferer(request.getHeader("Referer"));
        log.setVisitTime(LocalDateTime.now());

        visitorLogMapper.insert(log);
    }

    @Override
    public VisitorStatsDto getStats(Long userId) {
        VisitorStatsDto stats = new VisitorStatsDto();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime last7Days = now.minusDays(7);
        LocalDateTime last30Days = now.minusDays(30);

        // 今日访问量
        stats.setTodayViews(visitorLogMapper.countVisitsSince(userId, todayStart));

        // 今日访客数
        stats.setTodayVisitors(visitorLogMapper.countUniqueVisitorsSince(userId, todayStart));

        // 总访问量
        Long totalViews = visitorLogMapper.selectCount(
                new LambdaQueryWrapper<VisitorLog>()
                        .eq(VisitorLog::getUserId, userId));
        stats.setTotalViews(totalViews.intValue());

        // 总访客数（去重IP）
        Integer totalVisitors = visitorLogMapper.countUniqueVisitorsSince(userId, LocalDateTime.of(2020, 1, 1, 0, 0));
        stats.setTotalVisitors(totalVisitors);

        // 最近7天访问量
        stats.setLast7DaysViews(visitorLogMapper.countVisitsSince(userId, last7Days));

        // 最近30天访问量
        stats.setLast30DaysViews(visitorLogMapper.countVisitsSince(userId, last30Days));

        return stats;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
