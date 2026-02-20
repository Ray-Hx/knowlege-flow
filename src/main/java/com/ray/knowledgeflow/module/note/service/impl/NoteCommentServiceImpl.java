package com.ray.knowledgeflow.module.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.note.dto.CreateCommentDto;
import com.ray.knowledgeflow.module.note.dto.NoteCommentDto;
import com.ray.knowledgeflow.module.note.entity.NoteComment;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.mapper.NoteCommentMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.service.INoteCommentService;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 笔记评论服务实现类
 *
 * @author Ray
 * @since 2026-02-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteCommentServiceImpl implements INoteCommentService {

    private final NoteCommentMapper commentMapper;
    private final NoteEntryMapper noteMapper;
    private final UserMapper userMapper;

    @Override
    public List<NoteCommentDto> getComments(Long noteId) {
        // 验证笔记是否公开
        NoteEntry note = noteMapper.selectById(noteId);
        if (note == null || note.getIsPublic() == null || note.getIsPublic() != 1) {
            throw new BusinessException(404, "笔记不存在或未公开");
        }

        // 查询所有该笔记的评论（按时间升序）
        List<NoteComment> allComments = commentMapper.selectList(
                new LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, noteId)
                        .orderByAsc(NoteComment::getCreatedTime));

        if (allComments.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询用户信息
        Set<Long> userIds = allComments.stream().map(NoteComment::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 分层：顶层评论 + 子评论
        List<NoteComment> topLevel = allComments.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        Map<Long, List<NoteComment>> repliesMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(NoteComment::getParentId));

        return topLevel.stream()
                .map(comment -> {
                    NoteCommentDto dto = toDto(comment, userMap);
                    List<NoteComment> childComments = repliesMap.getOrDefault(comment.getId(), Collections.emptyList());
                    dto.setReplies(childComments.stream()
                            .map(child -> toDto(child, userMap))
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public NoteCommentDto addComment(Long userId, Long noteId, CreateCommentDto dto) {
        // 验证笔记是否公开
        NoteEntry note = noteMapper.selectById(noteId);
        if (note == null || note.getIsPublic() == null || note.getIsPublic() != 1) {
            throw new BusinessException(404, "笔记不存在或未公开");
        }

        // 如果是回复，验证父评论存在且属于同一笔记
        if (dto.getParentId() != null) {
            NoteComment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null || !parent.getNoteId().equals(noteId)) {
                throw new BusinessException(400, "父评论不存在");
            }
        }

        NoteComment comment = new NoteComment();
        comment.setNoteId(noteId);
        comment.setUserId(userId);
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent());
        comment.setCreatedTime(LocalDateTime.now());
        comment.setUpdatedTime(LocalDateTime.now());
        comment.setDeleted(0);
        commentMapper.insert(comment);

        // 查询用户信息并返回 DTO
        SysUser user = userMapper.selectById(userId);
        Map<Long, SysUser> userMap = user != null
                ? Map.of(userId, user)
                : Collections.emptyMap();

        NoteCommentDto result = toDto(comment, userMap);
        result.setReplies(Collections.emptyList());
        return result;
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        NoteComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除他人评论");
        }
        commentMapper.deleteById(commentId);
        // 同时删除该评论的所有子评论
        commentMapper.delete(new LambdaQueryWrapper<NoteComment>()
                .eq(NoteComment::getParentId, commentId));
    }

    @Override
    public long getCommentCount(Long noteId) {
        return commentMapper.selectCount(
                new LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, noteId));
    }

    private NoteCommentDto toDto(NoteComment comment, Map<Long, SysUser> userMap) {
        NoteCommentDto dto = new NoteCommentDto();
        dto.setId(comment.getId());
        dto.setNoteId(comment.getNoteId());
        dto.setParentId(comment.getParentId());
        dto.setContent(comment.getContent());
        dto.setUserId(comment.getUserId());
        dto.setCreatedTime(comment.getCreatedTime());

        SysUser user = userMap.get(comment.getUserId());
        if (user != null) {
            dto.setAuthorName(user.getUsername());
            dto.setAuthorAvatar(user.getAvatar());
        } else {
            dto.setAuthorName("匿名用户");
        }
        return dto;
    }
}
