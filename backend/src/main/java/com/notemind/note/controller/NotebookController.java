package com.notemind.note.controller;

import com.notemind.common.result.Result;
import com.notemind.framework.security.SecurityUtils;
import com.notemind.note.dto.SaveNotebookRequest;
import com.notemind.note.service.NotebookService;
import com.notemind.note.vo.NotebookVO;
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
@RequestMapping("/api/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    @GetMapping
    public Result<List<NotebookVO>> list() {
        return Result.success(notebookService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    public Result<NotebookVO> create(@Valid @RequestBody SaveNotebookRequest request) {
        return Result.success(notebookService.create(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/{id}")
    public Result<NotebookVO> update(@PathVariable Long id,
                                     @Valid @RequestBody SaveNotebookRequest request) {
        return Result.success(notebookService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestParam(defaultValue = "false") boolean force) {
        notebookService.delete(SecurityUtils.currentUserId(), id, force);
        return Result.success();
    }
}
