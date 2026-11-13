package com.notemind.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.note.dto.CreateNoteRequest;
import com.notemind.note.dto.MoveNoteRequest;
import com.notemind.note.dto.UpdateNoteRequest;
import com.notemind.note.entity.Note;
import com.notemind.note.entity.NoteFolder;
import com.notemind.note.entity.NoteLink;
import com.notemind.note.entity.NoteNotebook;
import com.notemind.note.mapper.NoteFolderMapper;
import com.notemind.note.mapper.NoteLinkMapper;
import com.notemind.note.mapper.NoteMapper;
import com.notemind.note.mapper.NoteNotebookMapper;
import com.notemind.note.service.NoteService;
import com.notemind.note.vo.NoteLinkVO;
import com.notemind.note.vo.NoteListItemVO;
import com.notemind.note.vo.NoteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Pattern LATIN_WORD = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern WIKILINK = Pattern.compile("\\[\\[([^\\[\\]]+)\\]]");
    private static final int MAX_LINKS = 200;
    private static final String DEFAULT_TITLE = "无标题笔记";

    private final NoteMapper noteMapper;
    private final NoteNotebookMapper notebookMapper;
    private final NoteFolderMapper folderMapper;
    private final NoteLinkMapper noteLinkMapper;

    @Override
    public List<NoteListItemVO> list(Long userId, Long notebookId, Long folderId, String keyword) {
        return noteMapper.selectListItems(userId, notebookId, folderId, keyword);
    }

    @Override
    public NoteVO get(Long userId, Long id) {
        return toVO(requireOwned(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO create(Long userId, CreateNoteRequest request) {
        requireNotebook(userId, request.getNotebookId());
        if (request.getFolderId() != null) {
            requireFolder(userId, request.getFolderId(), request.getNotebookId());
        }

        Note note = new Note();
        note.setUserId(userId);
        note.setNotebookId(request.getNotebookId());
        note.setFolderId(request.getFolderId());
        note.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : DEFAULT_TITLE);
        note.setContentMd(request.getContentMd() == null ? "" : request.getContentMd());
        note.setWordCount(countWords(note.getContentMd()));
        note.setPinned(false);
        noteMapper.insert(note);
        rebuildLinks(note);
        return toVO(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO update(Long userId, Long id, UpdateNoteRequest request) {
        Note note = requireOwned(userId, id);
        boolean changed = false;
        boolean contentChanged = false;
        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
            changed = true;
        }
        if (request.getContentMd() != null) {
            note.setContentMd(request.getContentMd());
            note.setWordCount(countWords(request.getContentMd()));
            changed = true;
            contentChanged = true;
        }
        if (request.getPinned() != null) {
            note.setPinned(request.getPinned());
            changed = true;
        }
        if (!changed) {
            return toVO(note);
        }
        // strictUpdateFill 只填充 null 字段，从数据库读出的 updatedAt 非 null，不会自动刷新
        note.setUpdatedAt(OffsetDateTime.now());
        noteMapper.updateById(note);
        if (contentChanged) {
            rebuildLinks(note);
        }
        return toVO(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        int affected = noteMapper.delete(new LambdaQueryWrapper<Note>()
                .eq(Note::getId, id)
                .eq(Note::getUserId, userId));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        // note_link 是派生数据，硬删；两端任一指向该笔记都要清理
        noteLinkMapper.delete(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getUserId, userId)
                .and(wrapper -> wrapper
                        .eq(NoteLink::getSourceNoteId, id)
                        .or()
                        .eq(NoteLink::getTargetNoteId, id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteVO move(Long userId, Long id, MoveNoteRequest request) {
        Note note = requireOwned(userId, id);
        requireNotebook(userId, request.getNotebookId());
        if (request.getFolderId() != null) {
            requireFolder(userId, request.getFolderId(), request.getNotebookId());
        }
        note.setNotebookId(request.getNotebookId());
        note.setFolderId(request.getFolderId());
        // updateById 忽略 null，移动到根目录（folderId=null）时必须显式 set
        noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, id)
                .eq(Note::getUserId, userId)
                .set(Note::getNotebookId, request.getNotebookId())
                .set(Note::getFolderId, request.getFolderId())
                .set(Note::getUpdatedAt, OffsetDateTime.now()));
        Note updated = noteMapper.selectById(id);
        if (updated == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        // 链接只在本笔记本内解析，换笔记本后需要按新范围重建
        rebuildLinks(updated);
        return toVO(updated);
    }

    @Override
    public List<NoteLinkVO> links(Long userId, Long noteId) {
        requireOwned(userId, noteId);
        return noteLinkMapper.selectOutgoing(userId, noteId);
    }

    @Override
    public List<NoteLinkVO> backlinks(Long userId, Long noteId) {
        requireOwned(userId, noteId);
        return noteLinkMapper.selectBacklinks(userId, noteId);
    }

    /**
     * 按正文重建出链。先清掉该笔记的旧链接，再把 [[标题]] 解析成同笔记本内未删除的笔记。
     * 标题重复时全部建链；改名后旧标题会失效，需重新保存源笔记。
     */
    private void rebuildLinks(Note note) {
        noteLinkMapper.delete(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getUserId, note.getUserId())
                .eq(NoteLink::getSourceNoteId, note.getId()));

        String content = note.getContentMd();
        if (!StringUtils.hasText(content)) {
            return;
        }
        Set<String> titles = new LinkedHashSet<>();
        Matcher matcher = WIKILINK.matcher(content);
        while (matcher.find() && titles.size() < MAX_LINKS) {
            String title = matcher.group(1).trim();
            if (!title.isEmpty()) {
                titles.add(title);
            }
        }
        if (titles.isEmpty()) {
            return;
        }

        List<Note> targets = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .select(Note::getId)
                .eq(Note::getUserId, note.getUserId())
                .eq(Note::getNotebookId, note.getNotebookId())
                .in(Note::getTitle, titles)
                .ne(Note::getId, note.getId()));
        for (Note target : targets) {
            NoteLink link = new NoteLink();
            link.setUserId(note.getUserId());
            link.setSourceNoteId(note.getId());
            link.setTargetNoteId(target.getId());
            link.setLinkType("wikilink");
            noteLinkMapper.insert(link);
        }
    }

    private Note requireOwned(Long userId, Long id) {
        Note note = noteMapper.selectOne(new LambdaQueryWrapper<Note>()
                .eq(Note::getId, id)
                .eq(Note::getUserId, userId));
        if (note == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        return note;
    }

    private void requireNotebook(Long userId, Long notebookId) {
        Long count = notebookMapper.selectCount(new LambdaQueryWrapper<NoteNotebook>()
                .eq(NoteNotebook::getId, notebookId)
                .eq(NoteNotebook::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(ErrorCode.NOTEBOOK_NOT_FOUND);
        }
    }

    private void requireFolder(Long userId, Long folderId, Long notebookId) {
        Long count = folderMapper.selectCount(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getId, folderId)
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getNotebookId, notebookId));
        if (count == 0) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
    }

    /** 与前端 `Notebook.vue` 的统计口径一致：每个汉字算一个词，连续的字母数字算一个词。 */
    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        Matcher cjkMatcher = CJK.matcher(text);
        while (cjkMatcher.find()) {
            cjk++;
        }
        int latin = 0;
        Matcher wordMatcher = LATIN_WORD.matcher(CJK.matcher(text).replaceAll(" "));
        while (wordMatcher.find()) {
            latin++;
        }
        return cjk + latin;
    }

    private NoteVO toVO(Note note) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setUserId(note.getUserId());
        vo.setNotebookId(note.getNotebookId());
        vo.setFolderId(note.getFolderId());
        vo.setTitle(note.getTitle());
        vo.setContentMd(note.getContentMd());
        vo.setWordCount(note.getWordCount());
        vo.setPinned(note.getPinned());
        vo.setCreatedAt(note.getCreatedAt());
        vo.setUpdatedAt(note.getUpdatedAt());
        return vo;
    }
}
