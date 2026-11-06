package com.notemind.note.controller;

import com.notemind.common.result.Result;
import com.notemind.framework.security.SecurityUtils;
import com.notemind.note.dto.CreateNoteRequest;
import com.notemind.note.dto.MoveNoteRequest;
import com.notemind.note.dto.UpdateNoteRequest;
import com.notemind.note.service.NoteService;
import com.notemind.note.vo.NoteLinkVO;
import com.notemind.note.vo.NoteListItemVO;
import com.notemind.note.vo.NoteVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public Result<List<NoteListItemVO>> list(@RequestParam Long notebookId,
                                             @RequestParam(required = false) Long folderId,
                                             @RequestParam(required = false) String keyword) {
        return Result.success(noteService.list(
                SecurityUtils.currentUserId(), notebookId, folderId, keyword));
    }

    @GetMapping("/{id}")
    public Result<NoteVO> get(@PathVariable Long id) {
        return Result.success(noteService.get(SecurityUtils.currentUserId(), id));
    }

    @PostMapping
    public Result<NoteVO> create(@Valid @RequestBody CreateNoteRequest request) {
        return Result.success(noteService.create(SecurityUtils.currentUserId(), request));
    }

    @PatchMapping("/{id}")
    public Result<NoteVO> update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateNoteRequest request) {
        return Result.success(noteService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteService.delete(SecurityUtils.currentUserId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/move")
    public Result<NoteVO> move(@PathVariable Long id,
                               @Valid @RequestBody MoveNoteRequest request) {
        return Result.success(noteService.move(SecurityUtils.currentUserId(), id, request));
    }

    @GetMapping("/{id}/links")
    public Result<List<NoteLinkVO>> links(@PathVariable Long id) {
        return Result.success(noteService.links(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/{id}/backlinks")
    public Result<List<NoteLinkVO>> backlinks(@PathVariable Long id) {
        return Result.success(noteService.backlinks(SecurityUtils.currentUserId(), id));
    }
}
