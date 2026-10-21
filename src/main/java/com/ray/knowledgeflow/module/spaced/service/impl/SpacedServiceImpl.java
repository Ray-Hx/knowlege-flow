package com.ray.knowledgeflow.module.spaced.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.common.utils.SM2Algorithm;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.ai.service.IAiService;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.spaced.dto.*;
import com.ray.knowledgeflow.module.spaced.entity.NoteReviewQuestion;
import com.ray.knowledgeflow.module.spaced.entity.SpacedReview;
import com.ray.knowledgeflow.module.spaced.mapper.NoteReviewQuestionMapper;
import com.ray.knowledgeflow.module.spaced.mapper.SpacedReviewMapper;
import com.ray.knowledgeflow.module.spaced.service.ISpacedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 间隔重复学习服务实现 (基于笔记)
 *
 * @author Ray
 * @since 2024-01-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpacedServiceImpl implements ISpacedService {

        private final SpacedReviewMapper reviewMapper;
        private final NoteReviewQuestionMapper questionMapper;
        private final NoteEntryMapper noteMapper;
        private final com.ray.knowledgeflow.module.spaced.mapper.SpacedLogMapper spacedLogMapper;
        private final IAiService aiService;

        @Override
        public List<ReviewNoteDto> getDueNotes(Long userId) {
                LocalDateTime now = LocalDateTime.now();

                // 获取到期的复习记录
                List<SpacedReview> dueReviews = reviewMapper.selectList(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId)
                                                .le(SpacedReview::getNextReviewTime, now)
                                                .orderByAsc(SpacedReview::getNextReviewTime));

                if (dueReviews.isEmpty()) {
                        return new ArrayList<>();
                }

                // 批量载入笔记与复习问题，避免逐条查询
                List<Long> noteIds = dueReviews.stream()
                                .map(SpacedReview::getNoteId)
                                .collect(Collectors.toList());

                Map<Long, NoteEntry> noteMap = noteMapper.selectBatchIds(noteIds).stream()
                                .collect(Collectors.toMap(NoteEntry::getId, n -> n));

                Map<Long, List<ReviewNoteDto.QuestionDto>> questionMap = questionMapper.selectList(
                                new LambdaQueryWrapper<NoteReviewQuestion>()
                                                .in(NoteReviewQuestion::getNoteId, noteIds))
                                .stream()
                                .collect(Collectors.groupingBy(NoteReviewQuestion::getNoteId,
                                                Collectors.mapping(q -> ReviewNoteDto.QuestionDto.builder()
                                                                .id(q.getId())
                                                                .question(q.getQuestion())
                                                                .answer(q.getAnswer())
                                                                .build(), Collectors.toList())));

                List<ReviewNoteDto> result = new ArrayList<>();

                for (SpacedReview review : dueReviews) {
                        // 笔记已删除时 selectBatchIds 不会返回（@TableLogic 自动过滤）
                        NoteEntry note = noteMap.get(review.getNoteId());
                        if (note == null) {
                                continue;
                        }

                        result.add(ReviewNoteDto.builder()
                                        .noteId(note.getId())
                                        .title(note.getTitle())
                                        .summary(note.getSummary())
                                        .repetitionCount(review.getRepetitionCount())
                                        .nextReviewTime(review.getNextReviewTime())
                                        .questions(questionMap.getOrDefault(note.getId(), new ArrayList<>()))
                                        .build());
                }

                return result;
        }

        @Override
        @Transactional
        public void submitReview(Long userId, Long noteId, ReviewResultDto dto) {
                SpacedReview review = reviewMapper.selectOne(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId)
                                                .eq(SpacedReview::getNoteId, noteId));

                if (review == null) {
                        log.warn("复习记录不存在: userId={}, noteId={}", userId, noteId);
                        return;
                }

                // 使用SM-2算法计算新的复习参数
                SM2Algorithm.SM2Result result = SM2Algorithm.calculate(
                                dto.getQuality(),
                                review.getRepetitionCount(),
                                review.getIntervalDays(),
                                review.getEasinessFactor());

                // 更新复习状态
                review.setRepetitionCount(result.getNewRepetitionCount());
                review.setIntervalDays(result.getNewIntervalDays());
                review.setEasinessFactor(result.getNewEasinessFactor());
                review.setNextReviewTime(result.getNextReviewTime());
                review.setLastReviewTime(LocalDateTime.now());

                reviewMapper.updateById(review);

                // 记录复习日志，用于活跃度统计
                com.ray.knowledgeflow.module.spaced.entity.SpacedLog logEntry = com.ray.knowledgeflow.module.spaced.entity.SpacedLog
                                .builder()
                                .noteId(noteId)
                                .userId(userId)
                                .reviewTime(LocalDateTime.now())
                                .quality(dto.getQuality())
                                .durationMs(0) // 暂时不追踪具体时长
                                .build();
                spacedLogMapper.insert(logEntry);

                log.info("提交笔记复习: noteId={}, quality={}, nextReview={}",
                                noteId, dto.getQuality(), result.getNextReviewTime());
        }

        @Override
        public SpacedStatsDto getStats(Long userId) {
                SpacedStatsDto stats = new SpacedStatsDto();
                LocalDateTime now = LocalDateTime.now();

                // 先获取所有未删除笔记的 ID 集合，确保统计与 getDueNotes 一致
                List<NoteEntry> activeNotes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .select(NoteEntry::getId));
                List<Long> activeNoteIds = activeNotes.stream()
                                .map(NoteEntry::getId)
                                .collect(Collectors.toList());

                if (activeNoteIds.isEmpty()) {
                        stats.setTotalCards(0);
                        stats.setDueToday(0);
                        stats.setMasteredCards(0);
                        stats.setReviewedToday(0);
                        stats.setTotalMinutes(0);
                        return stats;
                }

                // 总复习笔记数（仅限未删除笔记）
                Long totalNotes = reviewMapper.selectCount(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId)
                                                .in(SpacedReview::getNoteId, activeNoteIds));
                stats.setTotalCards(totalNotes.intValue());

                // 今日到期数（仅限未删除笔记）
                Long dueCount = reviewMapper.selectCount(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId)
                                                .le(SpacedReview::getNextReviewTime, now)
                                                .in(SpacedReview::getNoteId, activeNoteIds));
                stats.setDueToday(dueCount.intValue());

                // 已掌握数（重复次数 >= 3，仅限未删除笔记）
                Long masteredCount = reviewMapper.selectCount(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId)
                                                .ge(SpacedReview::getRepetitionCount, 3)
                                                .in(SpacedReview::getNoteId, activeNoteIds));
                stats.setMasteredCards(masteredCount.intValue());

                // 今日复习数（通过 lastReviewTime 判断）
                LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
                Long reviewedToday = reviewMapper.selectCount(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId)
                                                .ge(SpacedReview::getLastReviewTime, todayStart));
                stats.setReviewedToday(reviewedToday.intValue());
                stats.setTotalMinutes(0); // 不再追踪时长

                return stats;
        }

        @Override
        @Transactional
        public List<ReviewNoteDto.QuestionDto> generateQuestions(Long userId, Long noteId) {
                NoteEntry note = noteMapper.selectOne(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getId, noteId)
                                                .eq(NoteEntry::getUserId, userId));
                if (note == null) {
                        throw new BusinessException(404, "笔记不存在");
                }

                // 删除旧问题
                questionMapper.delete(
                                new LambdaQueryWrapper<NoteReviewQuestion>()
                                                .eq(NoteReviewQuestion::getNoteId, noteId));

                // 优先使用正文，若为空则降级为摘要
                String content = note.getContentMd();
                if (!org.springframework.util.StringUtils.hasText(content)) {
                        content = note.getSummary();
                }

                // 内容仍为空，直接返回提示性默认题
                if (!org.springframework.util.StringUtils.hasText(content)) {
                        log.warn("笔记内容为空，无法生成复习问题: noteId={}", noteId);
                        ReviewNoteDto.QuestionDto defaultQ = ReviewNoteDto.QuestionDto.builder()
                                        .question("请回顾《" + note.getTitle() + "》的主要内容，并用自己的话总结一下。")
                                        .answer("请参考笔记原文进行回顾。")
                                        .build();
                        NoteReviewQuestion entity = new NoteReviewQuestion();
                        entity.setNoteId(noteId);
                        entity.setQuestion(defaultQ.getQuestion());
                        entity.setAnswer(defaultQ.getAnswer());
                        questionMapper.insert(entity);
                        defaultQ.setId(entity.getId());
                        return List.of(defaultQ);
                }

                try {
                        // 调用专用的复习题生成方法（不复用 generateSummary，避免 prompt 被污染）
                        String aiResponse = aiService.generateReviewQuestions(note.getTitle(), content);

                        // 解析 AI 返回的 JSON
                        List<ReviewNoteDto.QuestionDto> questions = parseQuestions(aiResponse, noteId);

                        if (questions.isEmpty()) {
                                // AI 回了但解析失败，记录日志并生成兜底题
                                log.warn("AI 出题解析为空，使用兜底题: noteId={}, response={}", noteId,
                                                aiResponse.length() > 200 ? aiResponse.substring(0, 200) : aiResponse);
                                questions = List.of(ReviewNoteDto.QuestionDto.builder()
                                                .question("请用自己的语言概括《" + note.getTitle() + "》中最重要的知识点。")
                                                .answer(note.getSummary() != null ? note.getSummary() : "请回顾笔记内容")
                                                .build());
                        }

                        // 保存到缓存表
                        for (ReviewNoteDto.QuestionDto q : questions) {
                                NoteReviewQuestion entity = new NoteReviewQuestion();
                                entity.setNoteId(noteId);
                                entity.setQuestion(q.getQuestion());
                                entity.setAnswer(q.getAnswer());
                                questionMapper.insert(entity);
                                q.setId(entity.getId());
                        }

                        return questions;
                } catch (Exception e) {
                        log.error("AI 生成问题失败: noteId={}", noteId, e);
                        // 兜底：生成一道开放性回忆题
                        ReviewNoteDto.QuestionDto defaultQ = ReviewNoteDto.QuestionDto.builder()
                                        .question("请用自己的语言概括《" + note.getTitle() + "》中最重要的知识点。")
                                        .answer(note.getSummary() != null ? note.getSummary() : "请回顾笔记内容")
                                        .build();

                        NoteReviewQuestion entity = new NoteReviewQuestion();
                        entity.setNoteId(noteId);
                        entity.setQuestion(defaultQ.getQuestion());
                        entity.setAnswer(defaultQ.getAnswer());
                        questionMapper.insert(entity);
                        defaultQ.setId(entity.getId());

                        return List.of(defaultQ);
                }
        }

        @Override
        @Transactional
        public void initReviewQueue(Long userId) {
                // 获取用户所有未删除的笔记
                List<NoteEntry> notes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId));

                // 收集活跃笔记 ID
                List<Long> activeNoteIds = notes.stream()
                                .map(NoteEntry::getId)
                                .collect(Collectors.toList());

                // 清理已删除笔记的残留复习记录
                List<SpacedReview> allReviews = reviewMapper.selectList(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId));
                for (SpacedReview review : allReviews) {
                        if (!activeNoteIds.contains(review.getNoteId())) {
                                reviewMapper.deleteById(review.getId());
                                log.info("清理残留复习记录: noteId={}", review.getNoteId());
                        }
                }

                for (NoteEntry note : notes) {
                        addNoteToReview(userId, note.getId());
                }

                log.info("初始化用户复习队列: userId={}, noteCount={}", userId, notes.size());
        }

        @Override
        @Transactional
        public void addNoteToReview(Long userId, Long noteId) {
                // 检查是否已存在
                Long count = reviewMapper.selectCount(
                                new LambdaQueryWrapper<SpacedReview>()
                                                .eq(SpacedReview::getUserId, userId)
                                                .eq(SpacedReview::getNoteId, noteId));

                if (count > 0) {
                        return; // 已存在，跳过
                }

                // 创建初始复习状态
                SM2Algorithm.SM2Result initialState = SM2Algorithm.getInitialState();
                SpacedReview review = new SpacedReview();
                review.setUserId(userId);
                review.setNoteId(noteId);
                review.setNextReviewTime(initialState.getNextReviewTime());
                review.setIntervalDays(initialState.getNewIntervalDays());
                review.setRepetitionCount(initialState.getNewRepetitionCount());
                review.setEasinessFactor(initialState.getNewEasinessFactor());
                review.setCreatedTime(LocalDateTime.now());

                reviewMapper.insert(review);

                // 预生成问题
                generateQuestions(userId, noteId);
        }

        /**
         * 解析 AI 返回的问题列表
         */
        private List<ReviewNoteDto.QuestionDto> parseQuestions(String aiResponse, Long noteId) {
                List<ReviewNoteDto.QuestionDto> questions = new ArrayList<>();

                try {
                        // 尝试提取 JSON 数组
                        int start = aiResponse.indexOf('[');
                        int end = aiResponse.lastIndexOf(']');
                        if (start >= 0 && end > start) {
                                String jsonArray = aiResponse.substring(start, end + 1);
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                List<java.util.Map<String, String>> parsed = mapper.readValue(jsonArray,
                                                new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, String>>>() {
                                                });

                                for (java.util.Map<String, String> item : parsed) {
                                        String q = item.get("question");
                                        String a = item.get("answer");
                                        if (q != null && a != null) {
                                                questions.add(ReviewNoteDto.QuestionDto.builder()
                                                                .question(q)
                                                                .answer(a)
                                                                .build());
                                        }
                                }
                        }
                } catch (Exception e) {
                        log.warn("解析 AI 问题失败，将使用默认问题: {}", e.getMessage());
                }

                return questions;
        }
}
