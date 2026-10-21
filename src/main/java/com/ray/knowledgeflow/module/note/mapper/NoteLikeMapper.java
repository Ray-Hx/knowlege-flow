package com.ray.knowledgeflow.module.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.note.entity.NoteLike;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔记点赞 Mapper
 *
 * @author Ray
 * @since 2026-02-08
 */
@Mapper
public interface NoteLikeMapper extends BaseMapper<NoteLike> {

    /**
     * 统计笔记点赞数
     */
    @Select("SELECT COUNT(*) FROM note_like WHERE note_id = #{noteId}")
    Long countByNoteId(Long noteId);

    /**
     * 批量统计多篇笔记的点赞数，用于列表页一次性回源
     *
     * @return key 为 noteId，value 为点赞数；无人点赞的笔记不会出现在结果中
     */
    @MapKey("note_id")
    @Select("<script>"
            + "SELECT note_id, COUNT(*) AS cnt FROM note_like WHERE note_id IN "
            + "<foreach item='id' collection='noteIds' open='(' separator=',' close=')'>#{id}</foreach>"
            + " GROUP BY note_id"
            + "</script>")
    List<Map<String, Object>> countGroupByNoteIds(@Param("noteIds") List<Long> noteIds);

    /**
     * 检查用户是否已点赞
     */
    @Select("SELECT COUNT(*) FROM note_like WHERE note_id = #{noteId} AND user_id = #{userId}")
    boolean existsByNoteIdAndUserId(Long noteId, Long userId);

    /**
     * {@link #countGroupByNoteIds} 的结果转换为 noteId -> 点赞数
     */
    default Map<Long, Long> countByNoteIds(List<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : countGroupByNoteIds(noteIds)) {
            result.put(((Number) row.get("note_id")).longValue(),
                    ((Number) row.get("cnt")).longValue());
        }
        return result;
    }
}
