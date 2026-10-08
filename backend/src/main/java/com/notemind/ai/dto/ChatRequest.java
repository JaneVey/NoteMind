package com.notemind.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 对话请求。
 *
 * <p>说明：这是「AI 最小纵切验证」阶段的最小请求体，只含用户输入。
 * 后续接入会话持久化时会补充 {@code conversationId}，
 * 接入 RAG 时会补充 {@code knowledgeBaseId}（绑定知识库后切换为 RAG 模式）。
 */
public record ChatRequest(

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 8000, message = "单条消息不能超过 8000 字")
        String content
) {
}
