package com.notemind.ai.dto;

/**
 * SSE 流式输出的单个事件。
 *
 * <p>采用「显式类型 + 文本」的结构，而不是用 SSE 的 {@code event:} 字段区分，
 * 原因：前端 {@code utils/sse.ts} 基于 {@code fetch + ReadableStream} 手写解析，
 * 只能拿到 {@code data:} 的内容；把类型放进 JSON 里可以让同一套解析逻辑处理
 * 思考过程、正文、结束、错误四种事件，无需扩展解析器。
 *
 * <p>事件类型：
 * <ul>
 *   <li>{@link #REASONING} —— 模型思维链（DeepSeek 的 {@code reasoning_content}）</li>
 *   <li>{@link #CONTENT} —— 最终回答正文</li>
 *   <li>{@link #DONE} —— 本轮生成正常结束</li>
 *   <li>{@link #ERROR} —— 生成过程中出错（已捕获，连接不会异常断开）</li>
 * </ul>
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
