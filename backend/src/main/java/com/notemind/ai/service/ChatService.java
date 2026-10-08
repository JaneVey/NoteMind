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
 * 对话服务（AI 最小纵切验证版本）。
 *
 * <p><b>本类的两个关键点</b>
 *
 * <h3>1. 思考链的读取方式（已通过反编译 Spring AI 2.0.1 字节码确认）</h3>
 * {@code OpenAiChatModel} 内部维护常量
 * {@code REASONING_CONTENT = "reasoningContent"}，并把模型返回的
 * {@code reasoning_content}（DeepSeek 风格）或 {@code reasoning}（OpenAI 风格）
 * 放入 <b>消息元数据</b>。因此读取方式是：
 * <pre>
 *   response.getResult().getOutput().getMetadata().get("reasoningContent")
 * </pre>
 * 若该键为 null，说明当前模型未输出思考过程（例如非推理模型），并非错误。
 *
 * <h3>2. reasoning 分片可能是「累积值」而非「增量」</h3>
 * Spring AI 内部流式处理中存在 {@code accumulatedReasoning} 变量，
 * 意味着每个分片携带的可能是<b>截至当前的完整思考内容</b>，而不是新增部分。
 * 若直接把每片都推给前端，界面会疯狂重复。
 * 本类用 {@link ReasoningDelta} 统一处理两种情况：
 * 若新值以已发出内容为前缀则只取增量，否则视为独立增量。
 * 具体属于哪种，由启动时的 DEBUG 日志与实际观测确定（见开发日志）。
 *
 * <p><b>当前范围</b>：只做流式输出，不做会话持久化、不做 RAG 检索。
 * 后续把 {@code conversationId} / {@code knowledgeBaseId} 接进来即可扩展。
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
     * <p><b>为什么必须显式传思考参数（实测结论）</b>
     *
     * <p>对硅基流动的 {@code deepseek-ai/DeepSeek-V4-Flash} 实测发现：
     * <ul>
     *   <li><b>非流式</b>请求：默认就返回 {@code reasoning_content}</li>
     *   <li><b>流式</b>请求：默认<b>完全不返回</b>思考内容，必须显式传参</li>
     * </ul>
     * 实测各参数在流式下的效果：{@code thinking:{type:enabled}} 与
     * {@code enable_thinking:true} 均有效（分别产生 22 / 82 个思考分片），
     * 而 {@code include_reasoning:true} 无效。
     *
     * <p>另有一个反直觉的现象：{@code reasoning_effort:"high"} 在非流式下
     * 反而使模型不再输出思考（{@code reasoning_tokens:0}）。
     * 因此本项目默认不设置 reasoning-effort，而是通过 {@code extraBody}
     * 传供应商自己文档定义的思考开关。
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

    /**
     * 读取思考内容。优先取消息元数据，其次取 Generation 元数据，便于确认
     * Spring AI 究竟把该字段挂在哪一层（该结论会记入开发日志）。
     */
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
     * <p>兼容两种上游行为：
     * <ul>
     *   <li><b>累积</b>：每片携带截至当前的完整文本 → 只发出新增的后缀</li>
     *   <li><b>增量</b>：每片就是新增文本 → 原样发出</li>
     * </ul>
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
