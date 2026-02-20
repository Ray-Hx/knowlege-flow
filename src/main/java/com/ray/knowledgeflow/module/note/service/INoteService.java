package com.ray.knowledgeflow.module.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.module.note.dto.*;
import com.ray.knowledgeflow.module.user.dto.UserPublicProfileDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 笔记服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface INoteService {

    /**
     * 创建笔记
     *
     * @param userId 用户ID
     * @param dto    创建请求
     * @return 笔记详情
     */
    NoteDetailDto createNote(Long userId, CreateNoteDto dto);

    /**
     * 更新笔记（自动创建版本）
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param dto    更新请求
     * @return 笔记详情
     */
    NoteDetailDto updateNote(Long userId, Long noteId, UpdateNoteDto dto);

    /**
     * 删除笔记
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void deleteNote(Long userId, Long noteId);

    /**
     * 获取笔记详情
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 笔记详情
     */
    NoteDetailDto getNoteDetail(Long userId, Long noteId);

    /**
     * 分页查询笔记列表
     *
     * @param userId   用户ID
     * @param folderId 文件夹ID（可选）
     * @param page     页码
     * @param size     每页大小
     * @return 笔记列表
     */
    IPage<NoteListDto> listNotes(Long userId, Long folderId, Integer page, Integer size);

    /**
     * 搜索笔记
     *
     * @param userId  用户ID
     * @param keyword 关键词
     * @param page    页码
     * @param size    每页大小
     * @return 笔记列表
     */
    IPage<NoteListDto> searchNotes(Long userId, String keyword, Integer page, Integer size);

    /**
     * 获取用户的公开笔记列表
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 公开笔记列表
     */
    IPage<NoteListDto> listPublicNotes(Long userId, Integer page, Integer size);

    /**
     * 上传图片
     *
     * @param userId 用户ID
     * @param file   图片文件
     * @return 图片URL
     */
    String uploadImage(Long userId, MultipartFile file);

    /**
     * 重建用户所有笔记的向量索引（同步到知识库）
     *
     * @param userId 用户ID
     * @return 索引的笔记数量
     */
    int rebuildNoteIndex(Long userId);

    /**
     * 获取社区公开笔记列表
     *
     * @param keyword 搜索关键词（可选）
     * @param page    页码
     * @param size    每页大小
     * @return 笔记列表
     */
    IPage<NoteListDto> listCommunityNotes(String keyword, Integer page, Integer size);

    /**
     * 获取用户关注的博主的公开笔记流
     *
     * @param userId 当前用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 笔记列表
     */
    IPage<NoteListDto> listFollowingNotes(Long userId, Integer page, Integer size);

    /**
     * 获取公开笔记详情
     *
     * @param noteId 笔记ID
     * @return 笔记详情
     */
    NoteDetailDto getPublicNoteDetail(Long noteId);

    /**
     * 获取公开笔记详情（带当前用户信息）
     *
     * @param noteId        笔记ID
     * @param currentUserId 当前用户ID（可为null）
     * @return 笔记详情
     */
    NoteDetailDto getPublicNoteDetail(Long noteId, Long currentUserId);

    /**
     * 获取用户公开资料
     *
     * @param userId 用户ID
     * @return 用户公开资料 DTO
     */
    UserPublicProfileDto getUserPublicProfile(Long userId);

    /**
     * 获取相关公开笔记
     *
     * @param noteId 笔记ID
     * @param limit  最大返回数量
     * @return 相关笔记列表
     */
    java.util.List<NoteListDto> getRelatedPublicNotes(Long noteId, int limit);

    /**
     * 获取社区热门笔记排行榜
     *
     * @param limit 个数
     * @return 热门笔记列表
     */
    java.util.List<NoteListDto> getHotNotes(int limit);
}
