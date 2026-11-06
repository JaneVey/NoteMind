package com.notemind.framework.storage;

import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.common.result.Result;
import com.notemind.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 通用图片上传。scope 决定用途目录，后续笔记图片、头像直接复用，不必各写一套上传。
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final Set<String> ALLOWED_SCOPES = Set.of("notebook-icon", "note-image", "avatar");

    private final StorageService storageService;

    @PostMapping("/images")
    public Result<FileUrlVO> uploadImage(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "notebook-icon") String scope) {
        if (!ALLOWED_SCOPES.contains(scope)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的图片用途");
        }
        String folder = "public/" + scope + "/" + SecurityUtils.currentUserId();
        return Result.success(new FileUrlVO(storageService.uploadImage(file, folder)));
    }
}
