package com.ray.knowledgeflow.module.note.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 持久化笔记关系。MySQL 中的数据是双向链接的事实来源。
 */
@Data
@TableName("note_link")
public class NoteLink {

    public static final String TYPE_EXPLICIT = "EXPLICIT";
    public static final String TYPE_AI_RELATED = "AI_RELATED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long sourceNoteId;

    private Long targetNoteId;

    private String linkType;

    private String label;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
