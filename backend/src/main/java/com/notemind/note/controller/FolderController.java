package com.notemind.note.controller;

import com.notemind.common.result.Result;
import com.notemind.framework.security.SecurityUtils;
import com.notemind.note.dto.MoveFolderRequest;
import com.notemind.note.dto.SaveFolderRequest;
import com.notemind.note.service.FolderService;
import com.notemind.note.vo.FolderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping
    public Result<List<FolderVO>> list(@RequestParam Long notebookId) {
        return Result.success(folderService.list(SecurityUtils.currentUserId(), notebookId));
    }

    @PostMapping
    public Result<FolderVO> create(@Valid @RequestBody SaveFolderRequest request) {
        return Result.success(folderService.create(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/{id}")
    public Result<FolderVO> update(@PathVariable Long id,
                                   @Valid @RequestBody SaveFolderRequest request) {
        return Result.success(folderService.update(SecurityUtils.currentUserId(), id, request));
    }

    @PostMapping("/{id}/move")
    public Result<FolderVO> move(@PathVariable Long id,
                                 @Valid @RequestBody MoveFolderRequest request) {
        return Result.success(folderService.move(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestParam(defaultValue = "false") boolean force) {
        folderService.delete(SecurityUtils.currentUserId(), id, force);
        return Result.success();
    }
}
