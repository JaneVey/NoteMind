package com.notemind.ai.dto;

/**
 * SSE 流式输出的单个事件。
 *
 * <p>类型必须放进 JSON，不要用 SSE 的 {@code event:} 字段区分 ——
 * 前端 {@code utils/sse.ts} 基于 {@code fetch + ReadableStream} 手写解析，
 * 只拿得到 {@code data:} 的内容。
 *
 * <p>{@link #REASONING} 是模型思维链（DeepSeek 的 {@code reasoning_content}），
 * {@link #ERROR} 是已在流内捕获的错误，连接不会异常断开。
 */
public record ChatStreamChunk(String type, String text) {

    public static final String REASONING = "reasoning";
    public static final String CONTENT = "content";
    public static final String DONE = "done";
    public static final String ERROR = "error";

    public static ChatStreamChunk reasoning(String text) {
        return new ChatStreamChunk(REASONING, text);
    }

    public static ChatStreamChunk content(String text) {
        return new ChatStreamChunk(CONTENT, text);
    }

    public static ChatStreamChunk done() {
        return new ChatStreamChunk(DONE, null);
    }

    public static ChatStreamChunk error(String message) {
        return new ChatStreamChunk(ERROR, message);
    }
}
