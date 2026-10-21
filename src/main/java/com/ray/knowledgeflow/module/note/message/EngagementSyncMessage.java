package com.ray.knowledgeflow.module.note.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 互动计数同步消息。
 *
 * <p>只携带 noteId，消费端回源数据库重算计数。这样消息天然幂等，
 * 重复投递或乱序都不会导致计数错误——不传增量值正是为此。
 *
 * @author Ray
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngagementSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 需要刷新计数缓存的笔记 ID
     */
    private Long noteId;

    /**
     * 触发变更的用户 ID，用于失效该用户的"是否已点赞"缓存
     */
    private Long userId;

    /**
     * 触发动作，仅用于日志排查：LIKE / UNLIKE
     */
    private String action;
}
