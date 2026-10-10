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
 * <p>路径要与前端 {@code src/api/ai.ts} 的 {@code URL_SEND_CHAT} 一致。
 * 直接返回 Spring AI 的 Reactor {@code Flux}（Spring MVC 在类路径有 spring-webflux 时
 * 即可适配），比包一层 {@code SseEmitter} 省事，也天然有背压。需要 Bearer token。
 *
 * <p>流式过程中出错不要往外抛 —— 前端只会看到连接断开、拿不到原因，
 * 要由 {@link ChatService} 发一个 {@code type=error} 事件。
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
