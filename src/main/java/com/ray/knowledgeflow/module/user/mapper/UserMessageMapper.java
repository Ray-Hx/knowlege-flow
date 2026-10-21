package com.ray.knowledgeflow.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.user.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ray.knowledgeflow.module.user.dto.message.MessageConversationDto;

import java.util.List;

/**
 * 用户私信 Mapper 接口
 *
 * @author Ray
 * @since 2024-01-16
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {

    /**
     * 在数据库中完成每个联系人的最新消息与未读数聚合，
     * 避免把用户全部历史消息加载进 JVM。
     */
    @Select("""
            WITH ranked_messages AS (
                SELECT
                    CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS contact_id,
                    content,
                    created_time,
                    ROW_NUMBER() OVER (
                        PARTITION BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END
                        ORDER BY created_time DESC, id DESC
                    ) AS row_num,
                    SUM(CASE WHEN receiver_id = #{userId} AND is_read = 0 THEN 1 ELSE 0 END) OVER (
                        PARTITION BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END
                    ) AS unread_count
                FROM user_message
                WHERE deleted = 0 AND (sender_id = #{userId} OR receiver_id = #{userId})
            )
            SELECT
                r.contact_id AS contactId,
                COALESCE(u.username, '未知用户') AS contactName,
                u.avatar AS contactAvatar,
                r.content AS lastMessage,
                r.created_time AS lastTime,
                r.unread_count AS unreadCount
            FROM ranked_messages r
            LEFT JOIN sys_user u ON u.id = r.contact_id AND u.deleted = 0
            WHERE r.row_num = 1
            ORDER BY r.created_time DESC
            LIMIT #{limit}
            """)
    List<MessageConversationDto> selectConversationSummaries(
            @Param("userId") Long userId,
            @Param("limit") int limit);
}
