package com.ray.knowledgeflow.module.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.note.entity.NoteFolder;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 笔记文件夹 Mapper 接口
 * </p>
 *
 * @author Ray
 * @since 2024-01-16
 */
@Mapper
public interface NoteFolderMapper extends BaseMapper<NoteFolder> {
}
