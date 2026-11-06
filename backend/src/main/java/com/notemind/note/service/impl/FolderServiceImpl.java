package com.notemind.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.note.dto.MoveFolderRequest;
import com.notemind.note.dto.SaveFolderRequest;
import com.notemind.note.entity.Note;
import com.notemind.note.entity.NoteFolder;
import com.notemind.note.entity.NoteLink;
import com.notemind.note.entity.NoteNotebook;
import com.notemind.note.mapper.NoteFolderMapper;
import com.notemind.note.mapper.NoteLinkMapper;
import com.notemind.note.mapper.NoteMapper;
import com.notemind.note.mapper.NoteNotebookMapper;
import com.notemind.note.service.FolderService;
import com.notemind.note.vo.FolderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private static final int MAX_DEPTH = 5;

    private final NoteFolderMapper folderMapper;
    private final NoteNotebookMapper notebookMapper;
    private final NoteMapper noteMapper;
    private final NoteLinkMapper noteLinkMapper;

    @Override
    public List<FolderVO> list(Long userId, Long notebookId) {
        List<NoteFolder> folders = folderMapper.selectList(
                new LambdaQueryWrapper<NoteFolder>()
                        .eq(NoteFolder::getUserId, userId)
                        .eq(NoteFolder::getNotebookId, notebookId)
                        .orderByAsc(NoteFolder::getSortOrder)
                        .orderByAsc(NoteFolder::getId));
        return folders.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FolderVO create(Long userId, SaveFolderRequest request) {
        requireNotebook(userId, request.getNotebookId());
        requireParent(userId, request.getParentId(), request.getNotebookId());
        NoteFolder folder = new NoteFolder();
        folder.setUserId(userId);
        folder.setNotebookId(request.getNotebookId());
        folder.setParentId(request.getParentId());
        folder.setName(request.getName());
        folder.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        folderMapper.insert(folder);
        return toVO(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FolderVO update(Long userId, Long id, SaveFolderRequest request) {
        NoteFolder folder = requireOwned(userId, id);
        folder.setName(request.getName());
        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }
        // strictUpdateFill 只填充 null 字段，从数据库读出的 updatedAt 非 null，不会自动刷新
        folder.setUpdatedAt(OffsetDateTime.now());
        folderMapper.updateById(folder);
        return toVO(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FolderVO move(Long userId, Long id, MoveFolderRequest request) {
        NoteFolder folder = requireOwned(userId, id);
        Long parentId = request.getParentId();
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "不能移动到自身");
            }
            NoteFolder parent = requireParent(userId, parentId, folder.getNotebookId());
            if (collectSubtree(folder).contains(parentId)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "不能移动到自己的子文件夹");
            }
            // 移动后整棵子树不能超过 5 层
            if (parent == null || depthOf(parent) + subtreeHeight(folder) > MAX_DEPTH) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "移动后文件夹层级不能超过 5 层");
            }
        }
        folderMapper.update(null, new LambdaUpdateWrapper<NoteFolder>()
                .eq(NoteFolder::getId, id)
                .eq(NoteFolder::getUserId, userId)
                .set(NoteFolder::getParentId, parentId)
                .set(NoteFolder::getUpdatedAt, OffsetDateTime.now()));
        return toVO(folderMapper.selectById(id));
    }

    /**
     * 空文件夹直接删除；非空时默认拒绝，force=true 时连同子文件夹与笔记一起软删。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id, boolean force) {
        NoteFolder folder = requireOwned(userId, id);
        Set<Long> subtreeIds = collectSubtree(folder);

        Long noteCount = noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .in(Note::getFolderId, subtreeIds));
        int childCount = subtreeIds.size() - 1;
        if (!force && (noteCount > 0 || childCount > 0)) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_EMPTY,
                    "文件夹下还有 " + noteCount + " 篇笔记、" + childCount + " 个子文件夹");
        }

        if (force) {
            List<Long> noteIds = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                            .select(Note::getId)
                            .eq(Note::getUserId, userId)
                            .in(Note::getFolderId, subtreeIds))
                    .stream().map(Note::getId).toList();
            if (!noteIds.isEmpty()) {
                noteLinkMapper.delete(new LambdaQueryWrapper<NoteLink>()
                        .eq(NoteLink::getUserId, userId)
                        .and(wrapper -> wrapper
                                .in(NoteLink::getSourceNoteId, noteIds)
                                .or()
                                .in(NoteLink::getTargetNoteId, noteIds)));
            }
            noteMapper.delete(new LambdaQueryWrapper<Note>()
                    .eq(Note::getUserId, userId)
                    .in(Note::getFolderId, subtreeIds));
            folderMapper.delete(new LambdaQueryWrapper<NoteFolder>()
                    .eq(NoteFolder::getUserId, userId)
                    .in(NoteFolder::getId, subtreeIds));
            return;
        }
        folderMapper.deleteById(id);
    }

    /** 以目标文件夹为根，收集它自己和全部子孙文件夹的 id。 */
    private Set<Long> collectSubtree(NoteFolder root) {
        List<NoteFolder> all = folderMapper.selectList(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getUserId, root.getUserId())
                .eq(NoteFolder::getNotebookId, root.getNotebookId()));
        Map<Long, List<Long>> children = new HashMap<>();
        for (NoteFolder folder : all) {
            if (folder.getParentId() != null) {
                children.computeIfAbsent(folder.getParentId(), key -> new ArrayList<>())
                        .add(folder.getId());
            }
        }
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(root.getId());
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!result.add(current)) {
                continue;
            }
            children.getOrDefault(current, List.of()).forEach(queue::add);
        }
        return result;
    }

    private NoteFolder requireOwned(Long userId, Long id) {
        NoteFolder folder = folderMapper.selectOne(
                new LambdaQueryWrapper<NoteFolder>()
                        .eq(NoteFolder::getId, id)
                        .eq(NoteFolder::getUserId, userId));
        if (folder == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        return folder;
    }

    private void requireNotebook(Long userId, Long notebookId) {
        Long count = notebookMapper.selectCount(
                new LambdaQueryWrapper<NoteNotebook>()
                        .eq(NoteNotebook::getId, notebookId)
                        .eq(NoteNotebook::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(ErrorCode.NOTEBOOK_NOT_FOUND);
        }
    }

    /** 父文件夹必须属于同一笔记本；层级最多 5 层，避免出现 UI 难以展示的深度。 */
    private NoteFolder requireParent(Long userId, Long parentId, Long notebookId) {
        if (parentId == null) {
            return null;
        }
        NoteFolder parent = folderMapper.selectOne(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getId, parentId)
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getNotebookId, notebookId));
        if (parent == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        if (depthOf(parent) >= MAX_DEPTH) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件夹层级不能超过 5 层");
        }
        return parent;
    }

    /** 文件夹自身所在的层级，根层为 1。 */
    private int depthOf(NoteFolder folder) {
        int depth = 1;
        Long cursorId = folder.getParentId();
        while (cursorId != null) {
            depth++;
            NoteFolder cursor = folderMapper.selectOne(new LambdaQueryWrapper<NoteFolder>()
                    .eq(NoteFolder::getId, cursorId)
                    .eq(NoteFolder::getUserId, folder.getUserId()));
            if (cursor == null) {
                break;
            }
            cursorId = cursor.getParentId();
        }
        return depth;
    }

    /** 以该文件夹为根的子树高度，只有自己时为 1。 */
    private int subtreeHeight(NoteFolder root) {
        List<NoteFolder> all = folderMapper.selectList(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getUserId, root.getUserId())
                .eq(NoteFolder::getNotebookId, root.getNotebookId()));
        Map<Long, List<NoteFolder>> children = new HashMap<>();
        for (NoteFolder folder : all) {
            if (folder.getParentId() != null) {
                children.computeIfAbsent(folder.getParentId(), key -> new ArrayList<>())
                        .add(folder);
            }
        }
        return height(root, children);
    }

    private int height(NoteFolder folder, Map<Long, List<NoteFolder>> children) {
        int max = 0;
        for (NoteFolder child : children.getOrDefault(folder.getId(), List.of())) {
            max = Math.max(max, height(child, children));
        }
        return max + 1;
    }

    private FolderVO toVO(NoteFolder folder) {
        FolderVO vo = new FolderVO();
        vo.setId(folder.getId());
        vo.setUserId(folder.getUserId());
        vo.setNotebookId(folder.getNotebookId());
        vo.setParentId(folder.getParentId());
        vo.setName(folder.getName());
        vo.setSortOrder(folder.getSortOrder());
        vo.setCreatedAt(folder.getCreatedAt());
        vo.setUpdatedAt(folder.getUpdatedAt());
        return vo;
    }
}
