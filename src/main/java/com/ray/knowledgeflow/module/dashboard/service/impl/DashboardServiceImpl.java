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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        private final SpacedReviewMapper reviewMapper;

        @Override
        public DashboardStatsDto getStats(Long userId) {
                // 总笔记数
                Long totalNotes = noteMapper.selectCount(
                                new LambdaQueryWrapper<NoteEntry>()
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

                // 查询过去365天的笔记
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .ge(NoteEntry::getCreatedTime, yearStart.atStartOfDay()));

                // 按日期分组统计
                Map<LocalDate, Long> dailyNotes = notes.stream()
                                .filter(n -> n.getCreatedTime() != null)
                                .collect(Collectors.groupingBy(
                                                n -> n.getCreatedTime().toLocalDate(),
                                                Collectors.counting()));

                // 生成365天数据
                List<DailyStudyDto> result = new ArrayList<>();
                for (int i = 364; i >= 0; i--) {
                        LocalDate date = today.minusDays(i);
                        result.add(DailyStudyDto.builder()
                                        .date(date.toString())
                                        .noteCount(dailyNotes.getOrDefault(date, 0L).intValue())
                                        .build());
                }
                return result;
        }

        @Override
        public List<com.ray.knowledgeflow.module.dashboard.dto.TagStatsDto> getTagDistribution(Long userId) {
                // 查询用户所有笔记的标签字段（逗号分隔）
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .isNotNull(NoteEntry::getTags)
                                                .ne(NoteEntry::getTags, ""));

                // 解析所有标签，统计词频
                Map<String, Integer> tagCountMap = new java.util.HashMap<>();
                for (NoteEntry note : notes) {
                        if (note.getTags() == null || note.getTags().isBlank())
                                continue;
                        String[] tags = note.getTags().split("[,，]");
                        for (String tag : tags) {
                                String t = tag.trim();
                                if (!t.isEmpty()) {
                                        tagCountMap.merge(t, 1, Integer::sum);
                                }
                        }
                }

                // 取 Top 10，按数量降序
                return tagCountMap.entrySet().stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .limit(10)
                                .map(e -> com.ray.knowledgeflow.module.dashboard.dto.TagStatsDto.builder()
                                                .tag(e.getKey())
                                                .noteCount(e.getValue())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public List<DailyStudyDto> getLearningTrend(Long userId) {
                LocalDate today = LocalDate.now();
                // 取近12周（从84天前的周一开始）
                LocalDate start = today.minusWeeks(11).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
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
}
