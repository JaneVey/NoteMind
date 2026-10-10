package com.notemind.ai.service;

import com.notemind.ai.dto.ChatStreamChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对话服务（流式）。
 *
 * <p>思考内容在 Spring AI 里挂在消息元数据的 {@code reasoningContent} 键上
 * （{@code OpenAiChatModel} 内部常量），该键为 null 只说明当前模型没输出思考，不是错误。
 *
 * <p>注意 reasoning 分片可能是累积值而非增量，见 {@link ReasoningDelta}。
 */
@Slf4j
@Service
public class ChatService {

    /** 与 Spring AI OpenAiChatModel 内部常量保持一致 */
    private static final String REASONING_KEY = "reasoningContent";

    private final ChatClient chatClient;
    private final String reasoningEffort;
    private final String thinkingParam;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       @Value("${app.ai.reasoning-effort:}") String reasoningEffort,
                       @Value("${app.ai.thinking-param:none}") String thinkingParam) {
        this.chatClient = chatClientBuilder.build();
        this.reasoningEffort = reasoningEffort;
        this.thinkingParam = thinkingParam;
        log.info("ChatService 初始化：thinking-param={} reasoning-effort={}",
                thinkingParam,
                StringUtils.hasText(reasoningEffort) ? reasoningEffort : "(未设置)");
    }

    /**
     * 构造请求参数。
     *
     * <p>流式请求默认不返回思考内容，必须显式传思考开关；实测
     * {@code thinking:{type:enabled}} 与 {@code enable_thinking:true} 有效，{@code include_reasoning} 无效。
     * 另外 {@code reasoning_effort:"high"} 在非流式下反而会让模型不输出思考，所以默认不设 reasoning-effort。
     */
    private OpenAiChatOptions.Builder buildOptions() {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();

        Map<String, Object> extra = switch (thinkingParam == null ? "none" : thinkingParam.toLowerCase()) {
            // DeepSeek 官方文档写法
            case "thinking" -> Map.of("thinking", Map.of("type", "enabled"));
            // 部分 OpenAI 兼容平台（含 Qwen 系列）的写法
            case "enable_thinking" -> Map.of("enable_thinking", true);
            default -> Map.of();
        };
        if (!extra.isEmpty()) {
            builder.extraBody(extra);
        }

        if (StringUtils.hasText(reasoningEffort)) {
            builder.reasoningEffort(reasoningEffort);
        }
        return builder;
    }

    /**
     * 流式对话。
     *
     * @param userMessage 用户输入
     * @return 事件流，以 {@link ChatStreamChunk#DONE} 正常结束；
     * 出错时不抛异常，而是发出一个 {@code ERROR} 事件后结束，
     * 避免前端只看到连接被切断而拿不到原因。
     */
    public Flux<ChatStreamChunk> stream(String userMessage) {
        // 注意：状态必须随每次调用创建（不能是字段），否则并发请求会互相污染
        ReasoningDelta reasoningDelta = new ReasoningDelta();

        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .user(userMessage)
                .options(buildOptions());

        return spec.stream()
                .chatResponse()
                // 用 concatMap 而非 flatMap：flatMap 不保证顺序，会导致文字乱序
                .concatMap(response -> Flux.fromIterable(toChunks(response, reasoningDelta)))
                .concatWith(Flux.just(ChatStreamChunk.done()))
                .onErrorResume(e -> {
                    log.error("流式对话失败", e);
                    return Flux.just(ChatStreamChunk.error(
                            e.getMessage() != null ? e.getMessage() : "模型调用失败"));
                });
    }

    private List<ChatStreamChunk> toChunks(ChatResponse response, ReasoningDelta reasoningDelta) {
        List<ChatStreamChunk> chunks = new ArrayList<>(2);

        Generation generation = response.getResult();
        if (generation == null) {
            // 流末尾可能有一个只带 usage 的分片，没有 Generation
            return chunks;
        }

        AssistantMessage message = generation.getOutput();

        String rawReasoning = readReasoning(message, generation);
        if (StringUtils.hasText(rawReasoning)) {
            String delta = reasoningDelta.next(rawReasoning);
            if (StringUtils.hasText(delta)) {
                chunks.add(ChatStreamChunk.reasoning(delta));
            }
        }

        String text = message.getText();
        if (StringUtils.hasText(text)) {
            chunks.add(ChatStreamChunk.content(text));
        }

        return chunks;
    }

    /** 读取思考内容。优先取消息元数据，其次取 Generation 元数据 */
    private String readReasoning(AssistantMessage message, Generation generation) {
        Map<String, Object> messageMetadata = message.getMetadata();
        if (messageMetadata != null) {
            Object value = messageMetadata.get(REASONING_KEY);
            if (value != null) {
                return value.toString();
            }
        }
        // ChatGenerationMetadata 不是 Map，但提供了泛型 get(String)；
        // 无元数据时返回 ChatGenerationMetadata.NULL，其 get() 同样返回 null
        Object value = generation.getMetadata().get(REASONING_KEY);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    /**
     * 思考内容增量计算器。
     *
     * <p>上游行为不确定，两种都要兼容：新值以已发出内容为前缀时视为累积式，只取后缀；
     * 否则视为独立增量，原样发出。都把每片直接推送会导致界面疯狂重复。
     */
    static final class ReasoningDelta {

        private final StringBuilder emitted = new StringBuilder();

        String next(String raw) {
            String previous = emitted.toString();

            String delta;
            if (!previous.isEmpty() && raw.startsWith(previous)) {
                // 累积式：raw 是"已有内容 + 新增"
                delta = raw.substring(previous.length());
            } else if (raw.equals(previous)) {
                // 完全没有变化（例如模型在思考但该片无新增）
                delta = "";
            } else {
                // 增量式，或上游重置了内容
                delta = raw;
            }

            emitted.append(delta);

            if (log.isDebugEnabled()) {
                log.debug("reasoning 分片: raw长度={} 已发出={} 本次增量={}",
                        raw.length(), previous.length(), delta.length());
            }
            return delta;
        }
    }
}
