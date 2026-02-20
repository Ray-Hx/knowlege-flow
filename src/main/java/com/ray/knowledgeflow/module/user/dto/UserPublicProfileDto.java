package com.ray.knowledgeflow.module.user.dto;

import lombok.Data;

@Data
public class UserPublicProfileDto {
    private Long id;
    private String username;
    private String avatar;
    private String bio;
    private Long publicNoteCount;
    private Long totalViews;
    private Long totalLikes;

    // 关注与被关注
    private Long followerCount;
    private Long followingCount;
    private Boolean isFollowing;
}
