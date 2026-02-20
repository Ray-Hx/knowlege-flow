package com.ray.knowledgeflow.module.note.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 笔记版本历史实体类
 * </p>
 *
 * @author Ray
 * @since 2024-01-14
 */
@Data
@TableName("note_version")
public class NoteVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 笔记标题 (快照)
     */
    private String title;

    /**
     * 笔记内容 (快照)
     */
    private String contentMd;

    /**
     * 创建时间 (版本时间)
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
