package com.ray.knowledgeflow.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.dashboard.dto.DailyStudyDto;
import com.ray.knowledgeflow.module.dashboard.dto.DashboardStatsDto;
import com.ray.knowledgeflow.module.dashboard.dto.RecentNoteDto;
import com.ray.knowledgeflow.module.dashboard.service.IDashboardService;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.spaced.entity.SpacedReview;
import com.ray.knowledgeflow.module.spaced.mapper.SpacedReviewMapper;
import com.ray.knowledgeflow.module.ai.message.AiProcessProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

        private final NoteEntryMapper noteMapper;
        private final com.ray.knowledgeflow.module.note.mapper.NoteVersionMapper versionMapper;
        private final SpacedReviewMapper reviewMapper;
        private final com.ray.knowledgeflow.module.spaced.mapper.SpacedLogMapper spacedLogMapper;
        private final AiProcessProducer aiProcessProducer;
        private final StringRedisTemplate redisTemplate;

        @Override
        public DashboardStatsDto getStats(Long userId) {
                // 总笔记数
                Long totalNotes = noteMapper.selectCount(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .select(NoteEntry::getId, NoteEntry::getCreatedTime)
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0));

                // 待复习笔记数 (下次复习时间 <= 当前时间)
                Long pendingNotes = reviewMapper.selectCount(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId)
                                                .le(SpacedReview::getNextReviewTime, LocalDateTime.now()));

                // 本周新增笔记
                LocalDateTime weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                .atStartOfDay();
                Long weeklyNewNotes = noteMapper.selectCount(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .ge(NoteEntry::getCreatedTime, weekStart));

                // 今日新增笔记
                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                Long todayNewNotes = noteMapper.selectCount(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .ge(NoteEntry::getCreatedTime, todayStart));

                return DashboardStatsDto.builder()
                                .totalNotes(totalNotes)
                                .pendingNotes(pendingNotes)
                                .weeklyNewNotes(weeklyNewNotes)
                                .todayNewNotes(todayNewNotes)
                                .build();
        }

        @Override
        public List<DailyStudyDto> getWeeklyStudy(Long userId) {
                LocalDate today = LocalDate.now();
                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

                // 获取本周所有笔记
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .ge(NoteEntry::getCreatedTime, weekStart.atStartOfDay()));

                // 按日期分组统计笔记数
                Map<LocalDate, Long> dailyNotes = notes.stream()
                                .filter(n -> n.getCreatedTime() != null)
                                .collect(Collectors.groupingBy(
                                                n -> n.getCreatedTime().toLocalDate(),
                                                Collectors.counting()));

                // 生成一周数据
                List<DailyStudyDto> result = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                        LocalDate date = weekStart.plusDays(i);
                        result.add(DailyStudyDto.builder()
                                        .date(date.toString())
                                        .noteCount(dailyNotes.getOrDefault(date, 0L).intValue())
                                        .build());
                }
                return result;
        }

        @Override
        public List<DailyStudyDto> getMonthlyCalendar(Long userId, int year, int month) {
                LocalDate monthStart = LocalDate.of(year, month, 1);
                LocalDate monthEnd = monthStart.plusMonths(1);

                // 获取该月笔记
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .ge(NoteEntry::getCreatedTime, monthStart.atStartOfDay())
                                                .lt(NoteEntry::getCreatedTime, monthEnd.atStartOfDay()));

                // 按日期统计
                Map<LocalDate, Long> dailyNotes = notes.stream()
                                .filter(n -> n.getCreatedTime() != null)
                                .collect(Collectors.groupingBy(
                                                n -> n.getCreatedTime().toLocalDate(),
                                                Collectors.counting()));

                // 生成整月数据
                List<DailyStudyDto> result = new ArrayList<>();
                LocalDate date = monthStart;
                while (date.isBefore(monthEnd)) {
                        result.add(DailyStudyDto.builder()
                                        .date(date.toString())
                                        .noteCount(dailyNotes.getOrDefault(date, 0L).intValue())
                                        .build());
                        date = date.plusDays(1);
                }
                return result;
        }

        @Override
        public List<RecentNoteDto> getRecentNotes(Long userId, int limit) {
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .orderByDesc(NoteEntry::getUpdatedTime)
                                                .last("LIMIT " + limit));

                return notes.stream().map(note -> RecentNoteDto.builder()
                                .id(note.getId())
                                .title(note.getTitle())
                                .summary(note.getSummary())
                                .updatedTime(note.getUpdatedTime())
                                .updatedTimeText(formatTimeAgo(note.getUpdatedTime()))
                                .build()).collect(Collectors.toList());
        }

        /**
         * 将时间转换为友好显示格式
         */
        private String formatTimeAgo(LocalDateTime time) {
                if (time == null)
                        return "";

                long minutes = ChronoUnit.MINUTES.between(time, LocalDateTime.now());
                if (minutes < 1)
                        return "刚刚";
                if (minutes < 60)
                        return minutes + "分钟前";

                long hours = minutes / 60;
                if (hours < 24)
                        return hours + "小时前";

                long days = hours / 24;
                if (days < 7)
                        return days + "天前";

                return time.format(DateTimeFormatter.ofPattern("MM-dd"));
        }

        @Override
        public List<DailyStudyDto> getYearlyHeatmap(Long userId) {
                LocalDate today = LocalDate.now();
                LocalDate yearStart = today.minusDays(364);
                LocalDateTime yearStartDateTime = yearStart.atStartOfDay();

                // 1. 获取所有笔记的创建时间
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .ge(NoteEntry::getCreatedTime, yearStartDateTime));

                // 2. 获取所有笔记的编辑时间 (通过 NoteVersion)
                // 先获取用户所有笔记 ID
                List<NoteEntry> allUserNotes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .select(NoteEntry::getId)
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0));
                List<Long> noteIds = allUserNotes.stream().map(NoteEntry::getId).collect(Collectors.toList());

                List<com.ray.knowledgeflow.module.note.entity.NoteVersion> versions = new ArrayList<>();
                if (!noteIds.isEmpty()) {
                        versions = versionMapper.selectList(
                                        new LambdaQueryWrapper<com.ray.knowledgeflow.module.note.entity.NoteVersion>()
                                                        .select(
                                                                        com.ray.knowledgeflow.module.note.entity.NoteVersion::getNoteId,
                                                                        com.ray.knowledgeflow.module.note.entity.NoteVersion::getCreatedTime)
                                                        .in(com.ray.knowledgeflow.module.note.entity.NoteVersion::getNoteId,
                                                                        noteIds)
                                                        .ge(com.ray.knowledgeflow.module.note.entity.NoteVersion::getCreatedTime,
                                                                        yearStartDateTime));
                }

                // 3. 获取复习时间 (通过 SpacedLog)
                List<com.ray.knowledgeflow.module.spaced.entity.SpacedLog> logs = spacedLogMapper.selectList(
                                new LambdaQueryWrapper<com.ray.knowledgeflow.module.spaced.entity.SpacedLog>()
                                                .select(
                                                                com.ray.knowledgeflow.module.spaced.entity.SpacedLog::getNoteId,
                                                                com.ray.knowledgeflow.module.spaced.entity.SpacedLog::getReviewTime)
                                                .eq(com.ray.knowledgeflow.module.spaced.entity.SpacedLog::getUserId,
                                                                userId)
                                                .ge(com.ray.knowledgeflow.module.spaced.entity.SpacedLog::getReviewTime,
                                                                yearStartDateTime));

                // 聚合所有日期数据
                Map<LocalDate, Set<String>> dailyActivityMap = new HashMap<>();

                // 统计创建 (Type: CREATE)
                for (NoteEntry n : notes) {
                        LocalDate date = n.getCreatedTime().toLocalDate();
                        dailyActivityMap.computeIfAbsent(date, k -> new HashSet<>()).add(n.getId().toString());
                }

                // 统计编辑 (Type: EDIT)
                for (com.ray.knowledgeflow.module.note.entity.NoteVersion v : versions) {
                        LocalDate date = v.getCreatedTime().toLocalDate();
                        dailyActivityMap.computeIfAbsent(date, k -> new HashSet<>()).add(v.getNoteId().toString());
                }

                // 统计复习 (Type: REVIEW)
                for (com.ray.knowledgeflow.module.spaced.entity.SpacedLog l : logs) {
                        LocalDate date = l.getReviewTime().toLocalDate();
                        dailyActivityMap.computeIfAbsent(date, k -> new HashSet<>()).add(l.getNoteId().toString());
                }

                // 生成 365 天数据
                List<DailyStudyDto> result = new ArrayList<>();
                for (int i = 364; i >= 0; i--) {
                        LocalDate date = today.minusDays(i);
                        int activityCount = dailyActivityMap.getOrDefault(date, Collections.emptySet()).size();
                        result.add(DailyStudyDto.builder()
                                        .date(date.toString())
                                        .noteCount(activityCount)
                                        .build());
                }
                return result;
        }

        @Override
        public List<com.ray.knowledgeflow.module.dashboard.dto.TagStatsDto> getTagDistribution(Long userId) {
                // 查询用户所有笔记
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .select(NoteEntry::getTags)
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0));

                // 解析所有标签，统计词频
                Map<String, Integer> tagCountMap = new java.util.HashMap<>();
                int uncategorizedCount = 0;

                for (NoteEntry note : notes) {
                        if (note.getTags() == null || note.getTags().isBlank()) {
                                uncategorizedCount++;
                                continue;
                        }
                        // 支持多种分隔符：逗号、分号、空格
                        String[] tags = note.getTags().split("[,，;；\\s]+");
                        for (String tag : tags) {
                                String t = tag.trim();
                                if (!t.isEmpty()) {
                                        tagCountMap.merge(t, 1, Integer::sum);
                                }
                        }
                }

                // 取 Top 10，按数量降序
                List<com.ray.knowledgeflow.module.dashboard.dto.TagStatsDto> result = tagCountMap.entrySet().stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .limit(10)
                                .map(e -> com.ray.knowledgeflow.module.dashboard.dto.TagStatsDto.builder()
                                                .tag(e.getKey())
                                                .noteCount(e.getValue())
                                                .build())
                                .collect(Collectors.toList());

                // 如果有未分类的笔记，且结果集不满 10 个，或者未分类占比很高，可以加入（这里简单处理：始终加入，如果 count > 0）
                if (uncategorizedCount > 0) {
                        // 自动触发一次同步（带频率限制）
                        triggerAutomaticSync(userId);

                        result.add(com.ray.knowledgeflow.module.dashboard.dto.TagStatsDto.builder()
                                        .tag("未分类")
                                        .noteCount(uncategorizedCount)
                                        .build());
                }

                return result;
        }

        @Override
        public List<DailyStudyDto> getLearningTrend(Long userId) {
                LocalDate today = LocalDate.now();
                // 取近12周（从84天前的周一开始）
                LocalDate start = today.minusWeeks(11).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .select(NoteEntry::getCreatedTime)
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .ge(NoteEntry::getCreatedTime, start.atStartOfDay()));

                // 按周分组统计
                Map<LocalDate, Long> weeklyNotes = notes.stream()
                                .filter(n -> n.getCreatedTime() != null)
                                .collect(Collectors.groupingBy(
                                                n -> n.getCreatedTime().toLocalDate()
                                                                .with(TemporalAdjusters
                                                                                .previousOrSame(DayOfWeek.MONDAY)),
                                                Collectors.counting()));

                // 生成12周数据
                List<DailyStudyDto> result = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                        LocalDate weekMonday = start.plusWeeks(i);
                        result.add(DailyStudyDto.builder()
                                        .date(weekMonday.toString())
                                        .noteCount(weeklyNotes.getOrDefault(weekMonday, 0L).intValue())
                                        .build());
                }
                return result;
        }

        @Override
        public void syncAiTags(Long userId) {
                String dispatchKey = "dashboard:sync:tags:dispatch:" + userId;
                Boolean acquired = redisTemplate.opsForValue()
                                .setIfAbsent(dispatchKey, "1", Duration.ofMinutes(10));
                if (!Boolean.TRUE.equals(acquired)) {
                        log.info("用户 {} 的 AI 标签同步仍在冷却期，跳过重复投递", userId);
                        return;
                }
                // 查询该用户所有没有标签的笔记
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .select(NoteEntry::getId, NoteEntry::getTitle,
                                                                NoteEntry::getContentMd)
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .and(w -> w.isNull(NoteEntry::getTags).or().eq(NoteEntry::getTags,
                                                                ""))
                                                .orderByAsc(NoteEntry::getId)
                                                .last("LIMIT 100"));

                log.info("用户 {} 开始同步 AI 标签，待处理笔记数: {}", userId, notes.size());

                for (NoteEntry note : notes) {
                        aiProcessProducer.sendTagExtractionTask(
                                        note.getId(),
                                        userId,
                                        note.getTitle(),
                                        note.getContentMd());
                }
        }

        private void triggerAutomaticSync(Long userId) {
                String syncKey = "dashboard:sync:tags:" + userId;
                // 每 10 分钟最多触发一次自动同步 (原来是 1 小时，为了提高及时性缩短)
                Boolean success = redisTemplate.opsForValue()
                                .setIfAbsent(syncKey, "1", Duration.ofMinutes(10));

                if (Boolean.TRUE.equals(success)) {
                        log.info("触发用户 {} 的自动 AI 标签同步", userId);
                        syncAiTags(userId);
                }
        }
}
