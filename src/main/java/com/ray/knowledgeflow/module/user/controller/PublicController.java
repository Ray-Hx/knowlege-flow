package com.ray.knowledgeflow.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.note.dto.NoteListDto;
import com.ray.knowledgeflow.module.note.service.INoteService;
import com.ray.knowledgeflow.module.user.dto.PublicUserProfileDto;
import com.ray.knowledgeflow.module.user.dto.UserProfileDto;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.service.IUserProfileService;
import com.ray.knowledgeflow.module.user.service.IUserService;
import com.ray.knowledgeflow.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/**
 * 公开访问控制器
 *
 * @author Ray
 * @since 2024-01-17
 */
@Tag(name = "公开访问")
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final IUserService userService;
    private final IUserProfileService userProfileService;
    private final INoteService noteService;

    @GetMapping("/profile/{username}")
    @Operation(summary = "获取用户公开资料")
    public Result<PublicUserProfileDto> getPublicProfile(@PathVariable String username) {
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (!userProfileService.isPublicProfile(user.getId())) {
            throw new BusinessException(404, "用户不存在或个人主页未公开");
        }

        UserProfileDto profile = userProfileService.getProfile(user.getId());

        PublicUserProfileDto dto = new PublicUserProfileDto();
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());
        if (profile != null) {
            dto.setBio(profile.getBio());
            dto.setLearningGoals(profile.getLearningGoals());
        }

        return Result.success(dto);
    }

    @GetMapping("/notes/{username}")
    @Operation(summary = "获取用户公开笔记")
    public Result<IPage<NoteListDto>> listPublicNotes(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return Result.success(noteService.listPublicNotes(user.getId(), page, size));
    }
}
