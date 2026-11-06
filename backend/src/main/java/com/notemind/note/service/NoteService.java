package com.notemind.note.service;

import com.notemind.note.dto.CreateNoteRequest;
import com.notemind.note.dto.MoveNoteRequest;
import com.notemind.note.dto.UpdateNoteRequest;
import com.notemind.note.vo.NoteLinkVO;
import com.notemind.note.vo.NoteListItemVO;
import com.notemind.note.vo.NoteVO;

import java.util.List;

public interface NoteService {

    List<NoteListItemVO> list(Long userId, Long notebookId, Long folderId, String keyword);

    NoteVO get(Long userId, Long id);

    NoteVO create(Long userId, CreateNoteRequest request);

    NoteVO update(Long userId, Long id, UpdateNoteRequest request);

    void delete(Long userId, Long id);

    NoteVO move(Long userId, Long id, MoveNoteRequest request);

    List<NoteLinkVO> links(Long userId, Long noteId);

    List<NoteLinkVO> backlinks(Long userId, Long noteId);
}
