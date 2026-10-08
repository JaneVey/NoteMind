package com.notemind.ai.controller;

import com.notemind.ai.dto.ChatRequest;
import com.notemind.ai.dto.ChatStreamChunk;
import com.notemind.ai.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 对话接口。
 *
 * <p>路径 {@code /api/ai/chat/stream} 与前端 {@code src/api/ai.ts} 中已有的
 * {@code URL_SEND_CHAT} 常量一致，前端无需改动接口地址。
 *
 * <p><b>为什么返回 {@code Flux} 而不是 {@code SseEmitter}</b>：
 * Spring AI 的流式能力本身就是 Reactor 的 {@code Flux}，
 * 直接把它（经映射后）返回给 Spring MVC 最省事，也天然获得背压支持。
 * Spring MVC 在类路径存在 spring-webflux 时即可适配响应式返回值，
 * 并按 {@code text/event-stream} 逐条写出。
 *
 * <p><b>鉴权</b>：本接口位于 {@code /api/ai/**} 之下，受 Spring Security 保护，
 * 需要携带 {@code Authorization: Bearer <token>}。
 *
 * <p><b>错误处理</b>：流式过程中出错不会抛出（抛了前端只看到连接断开、拿不到原因），
 * 而是由 {@link ChatService} 发出一个 {@code type=error} 事件。</p>
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatService chatService;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatStreamChunk> stream(@Valid @RequestBody ChatRequest request) {
        return chatService.stream(request.content());
    }
}
