package com.notemind.framework.storage;

import com.notemind.common.exception.BusinessException;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StorageServiceTest {

    private StorageService service;

    @BeforeEach
    void setUp() {
        service = new StorageService(mock(MinioClient.class));
        ReflectionTestUtils.setField(service, "endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(service, "bucket", "notemind");
    }

    @Test
    @DisplayName("非图片类型被拒绝")
    void rejectsNonImage() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.uploadImage(file, "public/notebook-icon/1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(1001);
    }

    @Test
    @DisplayName("超过 1MB 的图片被拒绝")
    void rejectsOversizeImage() {
        byte[] content = new byte[1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", content);

        assertThatThrownBy(() -> service.uploadImage(file, "public/notebook-icon/1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(1001);
    }

    @Test
    @DisplayName("上传返回可匿名访问的完整 URL")
    void buildsPublicUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        String url = service.uploadImage(file, "public/notebook-icon/1");

        assertThat(url)
                .startsWith("http://localhost:9000/notemind/public/notebook-icon/1/")
                .endsWith(".png");
    }
}
