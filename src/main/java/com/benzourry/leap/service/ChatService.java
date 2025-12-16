package com.benzourry.leap.service;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.*;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.benzourry.leap.config.Constant.IO_BASE_DOMAIN;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import static java.util.Arrays.asList;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final CognaRepository cognaRepository;
    private final CognaSourceRepository cognaSourceRepository;
    private final EntryAttachmentRepository entryAttachmentRepository;
    private final EntryService entryService;
    private final BucketService bucketService;
    private final LookupService lookupService;
    private final FormRepository formRepository;
    private final ItemRepository itemRepository;
    private final DatasetRepository datasetRepository;
    private final MailService mailService;
    private final LambdaService lambdaService;

    final String MILVUS = "milvus";
    final String CHROMADB = "chromadb";
    final String INMEMORY = "inmemory";
    private final UserRepository userRepository;

    @Value("${cogna.MILVUS_HOST:10.224.203.218}")
    String MILVUS_HOST;

    @Value("${cogna.MILVUS_PORT:19530}")
    int MILVUS_PORT;

    @Value("${cogna.MILVUS_USER:reka}")
    String MILVUS_USER;

    @Value("${cogna.MILVUS_PASSWORD:[milvu5]}")
    String MILVUS_PASSWORD;

    @Value("${cogna.CROMADB_HOST:http://10.224.203.218}")
    String CHROMA_BASEURL;

    @Value("${cogna.CROMADB_PORT:8001}")
    long CHROMA_PORT;
    final String COLLECTION_PREFIX = "cogna_";

    @Value("${cogna.LOCALAI_HOST:http://10.28.114.194:8080}")
    String DEFAULT_LOCALAI_BASEURL;

    @Value("${cogna.OLLAMA_HOST:http://10.28.114.194:11434}")
    String DEFAULT_OLLAMA_BASEURL;

    @Value("${spring.profiles.active}")
    String APP_INSTANCE;

    @Value("${cogna.onnx.image-classification.model-path}")
    String modelPath;

    Executor executor;

    private final ObjectMapper MAPPER;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${instance.scheduler.enabled:true}")
    boolean schedulerEnabled;
    private EmbeddingModel e5Small;
    private EmbeddingModel allMiniLm;
    private ChatService self;

    public ChatService(CognaRepository cognaRepository,
                       CognaSourceRepository cognaSourceRepository,
                       EntryAttachmentRepository entryAttachmentRepository,
                       EntryService entryService,
                       BucketService bucketService,
                       LookupService lookupService,
                       LambdaService lambdaService,
                       FormRepository formRepository,
                       ItemRepository itemRepository,
                       DatasetRepository datasetRepository,
                       MailService mailService,
                       @Qualifier("asyncExec") Executor executor,
                       @Lazy ChatService self,
                       UserRepository userRepository, ObjectMapper MAPPER) {
        this.cognaRepository = cognaRepository;
        this.cognaSourceRepository = cognaSourceRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.entryService = entryService;
        this.bucketService = bucketService;
        this.lookupService = lookupService;
        this.lambdaService = lambdaService;
        this.formRepository = formRepository;
        this.itemRepository = itemRepository;
        this.datasetRepository = datasetRepository;
        this.mailService = mailService;
        this.executor = executor;
//        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.userRepository = userRepository;
        this.self = self;

        this.MAPPER = MAPPER;
    }


    @PostConstruct
    public void init() {
        this.e5Small = new OnnxEmbeddingModel(
                modelPath + "/multilingual-e5-small/model_quantized.onnx",
                modelPath + "/multilingual-e5-small/tokenizer.json",
                PoolingMode.MEAN
        );
        this.allMiniLm = new OnnxEmbeddingModel(
                modelPath + "/all-minilm-l6-v2/model_quantized.onnx",
                modelPath + "/all-minilm-l6-v2/tokenizer.json",
                PoolingMode.MEAN
        );
    }

    interface TextProcessor {

        @SystemMessage("You are a translation engine. \n" +
                "CRITICAL INSTRUCTION:\n" +
                "- Detect the source language automatically and translate the text into {{language}}. \n" +
                "- Output only the translated text. \n" +
                "- Do not include explanations, source text, notes, or any other symbols or formatting.")
        @UserMessage("{{text}}")
        String translate(@V("text") String text, @UserMessage List<ImageContent> images, @V("language") String language);

        @UserMessage("Analyze {{what}} of the following text and classify it into either [{{classification}}]: {{text}}\n\n" +
                "Where \n{{classificationDesc}}")
        @SystemMessage("CRITICAL INSTRUCTION:\n" +
                "  - You must ONLY output TEXT from the following choices: [{{classification}}]\n" +
                "  - Do not include any explanatory text before or after the text\n" +
                "  - Do not use markdown code blocks\n" +
                "  - Do not include any additional formatting\n" +
                "STRICT RESPONSE FORMAT:\n" +
                "{{classificationMulti}} ({{classification}}) - no additional text or explanations\n")
        List<String> textClassification(@V("what") String what,
                                        @V("classification") String classification,
                                        @V("classificationDesc") String classificationDesc,
                                        @V("classificationMulti") String classificationMulti,
                                        @V("text") String text);

        @SystemMessage("You are a summarization engine. \n" +
                "CRITICAL INSTRUCTION:\n" +
                "- Your task is to summarize the given text clearly and concisely. \n" +
                "- Output only the summary, without explanations, comments, or additional formatting. \n" +
                "- Summarize every message from user in {{n}} bullet points. " +
                "- Provide only bullet points.")
        List<String> summarize(@UserMessage String text, @UserMessage List<ImageContent> images, @V("n") int n);

        @SystemMessage("Generate text from user message. {{n}}")
        String generate(@UserMessage String text, @UserMessage List<ImageContent> images, @V("n") String n);
    }

    public interface Assistant {

        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        @Agent("{{systemMessage}}. Today is {{current_date}}.")
        String chat(@UserMessage("userMessage") String userMessage, @UserMessage("contentList") List<Content> contentList, @V("systemMessage") String systemMessage);
    }

    public interface StreamingAssistant {
        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        TokenStream chat(@UserMessage("userMessage") String userMessage, @UserMessage("contentList") List<Content> contentList, @V("systemMessage") String systemMessage);
    }

    public interface SubAgent {

        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        @UserMessage("{{userMessage}}")
        @Agent
        String chat(@V("userMessage") String userMessage, @UserMessage List<Content> contentList, @V("systemMessage") String systemMessage);
    }


    public interface MasterAgent {
        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        @Agent
        String chat(@UserMessage String userMessage, @UserMessage List<Content> contentList, @V("systemMessage") String systemMessage);
    }

    Map<Long, Map<String, Assistant>> assistantHolder = new ConcurrentHashMap<>();
    Map<Long, SubAgent> agentHolder = new ConcurrentHashMap<>();
    Map<Long, TextProcessor> textProcessorHolder = new ConcurrentHashMap<>();
    Map<Long, Map<String, StreamingAssistant>> streamAssistantHolder = new ConcurrentHashMap<>();
    Map<Long, EmbeddingStore> storeHolder = new ConcurrentHashMap<>();

    public ChatModel getChatModel(Cogna cogna, String responseFormat) {
        return switch (cogna.getInferModelType()) {
            case "openai" -> {
                OpenAiChatModel.OpenAiChatModelBuilder oib = OpenAiChatModel.builder()
                        .apiKey(cogna.getInferModelApiKey())
                        .modelName(cogna.getInferModelName())
                        .temperature(cogna.getTemperature())
                        .responseFormat(responseFormat)
//                    .logResponses(true)
                        .logRequests(true)
                        .timeout(Duration.ofMinutes(10));

                if ("json_schema".equals(responseFormat)) {
                    oib.strictJsonSchema(true);
                }

                yield oib.build();
            }
            case "deepseek" -> {
                OpenAiChatModel.OpenAiChatModelBuilder oib = OpenAiChatModel.builder()
                        .apiKey(cogna.getInferModelApiKey())
                        .baseUrl("https://api.deepseek.com")
                        .modelName(cogna.getInferModelName())
                        .temperature(cogna.getTemperature())
                        .responseFormat(responseFormat)
//                    .logResponses(true)
                        .logRequests(true)
                        .timeout(Duration.ofMinutes(10));

                if ("json_schema".equals(responseFormat)) {
                    oib.strictJsonSchema(true);
                }

                yield oib.build();
            }
            case "gemini" -> {
                GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder oib = GoogleAiGeminiChatModel.builder()
                        .apiKey(cogna.getInferModelApiKey())
                        .modelName(cogna.getInferModelName())
                        .temperature(cogna.getTemperature())
                        .responseFormat("json_schema".equals(responseFormat) ? ResponseFormat.JSON : ResponseFormat.TEXT)
                        .logResponses(true)
                        .logRequests(true)
                        .timeout(Duration.ofMinutes(10));

//                if ("json_schema".equals(responseFormat)) {
//                    oib.strictJsonSchema(true);
//                }

                yield oib.build();
            }
            case "huggingface" -> {
                OpenAiChatModel.OpenAiChatModelBuilder oib = OpenAiChatModel.builder()
                        .apiKey(cogna.getInferModelApiKey())
                        .baseUrl("https://router.huggingface.co/v1")
                        .modelName(cogna.getInferModelName())
                        .temperature(cogna.getTemperature())
                        .responseFormat(responseFormat)
                        .logResponses(true)
                        .logRequests(true)
                        .timeout(Duration.ofMinutes(10));

                if ("json_schema".equals(responseFormat)) {
                    oib.strictJsonSchema(true);
                }

                yield oib.build();
            }
//            case "huggingface" -> HuggingFaceChatModel.builder()
//                    .accessToken(cogna.getInferModelApiKey())
//                    .modelId(cogna.getInferModelName())
//                    .temperature(cogna.getTemperature())
//                    .timeout(Duration.ofMinutes(10))
//                    .waitForModel(true)
//                    .build();
            case "localai" -> LocalAiChatModel.builder()
                    .modelName(cogna.getInferModelName())
                    .baseUrl(cogna.getData().at("/inferBaseUrl").asText(DEFAULT_LOCALAI_BASEURL))
                    .temperature(cogna.getTemperature())
                    .logRequests(true)
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "ollama" -> OllamaChatModel.builder()
                    .modelName(cogna.getInferModelName())
                    .baseUrl(cogna.getData().at("/inferBaseUrl").asText(DEFAULT_OLLAMA_BASEURL))
                    .temperature(cogna.getTemperature())
                    .responseFormat("json".equals(responseFormat) ? ResponseFormat.JSON : ResponseFormat.TEXT)
                    .logRequests(true)
                    .timeout(Duration.ofMinutes(10))
                    .build();
            /* UTK GEMINI
            case "vertex-ai-gemini" -> VertexAiGeminiChatModel.builder()
                    .project(cogna.getData().at("/project").asText())
                    .location(cogna.getData().at("/location").asText())
                    .modelName(cogna.getInferModelName())
                    .temperature(cogna.getTemperature().floatValue())
                    .build();*/
            default -> null;
        };
    }

    public StreamingChatModel getStreamingChatModel(Cogna cogna) {
        return switch (cogna.getInferModelType()) {
            case "openai" -> OpenAiStreamingChatModel.builder()
                    .apiKey(cogna.getInferModelApiKey())
                    .modelName(cogna.getInferModelName())
                    .temperature(cogna.getTemperature())
//                    .logResponses(true)
                    .logRequests(true)
//                    .maxTokens(cogna.getMaxToken())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "deepseek" -> OpenAiStreamingChatModel.builder()
                    .apiKey(cogna.getInferModelApiKey())
                    .baseUrl("https://api.deepseek.com")
                    .modelName(cogna.getInferModelName())
                    .temperature(cogna.getTemperature())
//                    .logResponses(true)
                    .logRequests(true)
//                    .maxTokens(cogna.getMaxToken())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "gemini" -> GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey(cogna.getInferModelApiKey())
//                    .baseUrl("https://api.deepseek.com")
                    .modelName(cogna.getInferModelName())
                    .temperature(cogna.getTemperature())
                    .logResponses(true)
                    .logRequests(true)
//                    .maxTokens(cogna.getMaxToken())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "huggingface" -> OpenAiStreamingChatModel.builder()
                    .apiKey(cogna.getInferModelApiKey())
                    .baseUrl("https://router.huggingface.co/v1")
                    .modelName(cogna.getInferModelName())
                    .temperature(cogna.getTemperature())
                    .logResponses(true)
                    .logRequests(true)
//                    .maxTokens(cogna.getMaxToken())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "localai" -> LocalAiStreamingChatModel.builder()
                    .modelName(cogna.getInferModelName())
                    .baseUrl(cogna.getData().at("/inferBaseUrl").asText(DEFAULT_LOCALAI_BASEURL))
                    .temperature(cogna.getTemperature())
//                    .logResponses(true)
                    .logRequests(true)
//                    .maxTokens(cogna.getMaxToken())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "ollama" -> OllamaStreamingChatModel.builder()
                    .modelName(cogna.getInferModelName())
                    .baseUrl(cogna.getData().at("/inferBaseUrl").asText(DEFAULT_OLLAMA_BASEURL))
                    .temperature(cogna.getTemperature())
//                    .responseFormat("json".equals(responseFormat)? ResponseFormat.JSON: ResponseFormat.TEXT)
                    .logRequests(true)
                    .timeout(Duration.ofMinutes(10))
                    .build();

            /* UTK GEMINI
            case "vertex-ai-gemini" -> VertexAiGeminiStreamingChatModel.builder()
                    .project(cogna.getData().at("/inferProject").asText())
                    .location(cogna.getData().at("/inferLocation").asText())
                    .modelName(cogna.getInferModelName())
                    .temperature(cogna.getTemperature().floatValue())
                    .build(); */
            default -> null;
        };
    }

    public ChatMemory getChatMemory(Cogna cogna, String email) {
        ChatMemory thisChatMemory = MessageWindowChatMemory.withMaxMessages(Optional.ofNullable(cogna.getMaxChatMemory()).orElse(5));

        Cache<String, ChatMemory> cache = getOrCreateCache(cogna.getId());
        // logger.info("#####################Chat memory cache: {}", cache);
        return cache.get(email, key -> thisChatMemory);
    }

    // Get or create inner cache for a chatbot
    Map<Long, Cache<String, ChatMemory>> chatMemoryMap = new ConcurrentHashMap<>();

    private Cache<String, ChatMemory> getOrCreateCache(Long chatbotId) {

        return chatMemoryMap.computeIfAbsent(chatbotId, id ->
                        Caffeine.newBuilder()
                                .maximumSize(1000)
                                .expireAfterAccess(Duration.ofHours(6))
                                .expireAfterWrite(Duration.ofHours(12))
                                .scheduler(Scheduler.systemScheduler())
                                .build()
        );
    }

    public EmbeddingModel getEmbeddingModel(Cogna cogna) {
        return switch (cogna.getEmbedModelType()) {
            case "minilm" -> allMiniLm;
//            case "e5large" -> e5Large;
            case "e5small" -> e5Small;
            case "openai" -> OpenAiEmbeddingModel.builder()
                    .apiKey(cogna.getEmbedModelApiKey())
                    .modelName(cogna.getEmbedModelName())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "huggingface" -> HuggingFaceEmbeddingModel.builder()
                    .accessToken(cogna.getEmbedModelApiKey())
                    .modelId(cogna.getEmbedModelName())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "localai" -> LocalAiEmbeddingModel.builder()
                    .baseUrl(cogna.getData().at("/embedBaseUrl").asText(DEFAULT_LOCALAI_BASEURL))
                    .modelName(cogna.getEmbedModelName())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            case "ollama" -> OllamaEmbeddingModel.builder()
                    .baseUrl(cogna.getData().at("/embedBaseUrl").asText(DEFAULT_OLLAMA_BASEURL))
                    .modelName(cogna.getEmbedModelName())
                    .timeout(Duration.ofMinutes(10))
                    .build();
            /* UNTUK GEMINI
            case "vertex-ai" -> VertexAiEmbeddingModel.builder()
                    .endpoint(cogna.getData().at("/embedEndpoint").asText("us-central1-aiplatform.googleapis.com:443"))
                    .project(cogna.getData().at("/embedProject").asText("langchain4j"))
                    .location(cogna.getData().at("/embedLocation").asText("us-central1"))
                    .publisher("google")
                    .modelName(cogna.getEmbedModelName())
                    .build(); */
//                    .accessToken(cogna.getEmbedModelApiKey())
//                    .modelId(cogna.getEmbedModelName())
//                    .timeout(Duration.ofMinutes(10))
//                    .build();
//            case "azureopenai" -> AzureOpenAiEmbeddingModel.builder()
//                    .apiKey(cogna.getEmbedModelApiKey())
//                    .build();
            default -> allMiniLm;
        };
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore(Cogna cogna) {

        logger.info("Collection: " + COLLECTION_PREFIX + APP_INSTANCE + "_" + cogna.getId() + ", DB:" + cogna.getVectorStoreType());

        if (storeHolder.get(cogna.getId()) != null) {
            return storeHolder.get(cogna.getId());
        } else {
            var store = switch (cogna.getVectorStoreType()) {
                case MILVUS -> {
                    String milvusHost = MILVUS_HOST;
                    Integer milvusPort = MILVUS_PORT;

                    if (cogna.getData().at("/overrideDb").asBoolean(false)) {
                        if (cogna.getVectorStoreHost() != null) milvusHost = cogna.getVectorStoreHost();
                        if (cogna.getVectorStorePort() != null) milvusPort = cogna.getVectorStorePort().intValue();
                    }

                    yield MilvusEmbeddingStore.builder()
                            .host(milvusHost)
                            .port(milvusPort)
                            .username(MILVUS_USER)
                            .password(MILVUS_PASSWORD)
                            .collectionName(COLLECTION_PREFIX + APP_INSTANCE + "_" + cogna.getId())
                            .dimension(switch (cogna.getEmbedModelType()) {
                                case "openai" -> 1536;
                                case "huggingface" -> 384;
                                case "minilm" -> 384;
                                case "e5small" -> 384;
                                case "vertex-ai" -> 768;
                                default -> 1536;
                            })
                            .build();
                }
                case CHROMADB -> {
                    String chromaHost = CHROMA_BASEURL;
                    Long chromaPort = CHROMA_PORT;

                    if (cogna.getData().at("/overrideDb").asBoolean(false)) {
                        if (cogna.getVectorStoreHost() != null) chromaHost = cogna.getVectorStoreHost();
                        if (cogna.getVectorStorePort() != null) chromaPort = cogna.getVectorStorePort();
                    }

                    yield ChromaEmbeddingStore.builder()
                            .baseUrl(chromaHost + ":" + chromaPort)
                            .collectionName(COLLECTION_PREFIX + APP_INSTANCE + "_" + cogna.getId())
                            .timeout(Duration.ofMinutes(10))
                            .build();
                }
                case INMEMORY -> {
                    InMemoryEmbeddingStore<TextSegment> inMemStore;
                    File inMemoryStore = new File(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-" + cogna.getId() + ".store");
                    if (inMemoryStore.isFile()) {
                        inMemStore = InMemoryEmbeddingStore.fromFile(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-" + cogna.getId() + ".store");
                    } else {
                        inMemStore = new InMemoryEmbeddingStore<>();
                    }
                    yield inMemStore;
                }
                default -> null;
            };

            storeHolder.put(cogna.getId(), store);
            return store;
        }
    }

    public List<Map<String, Object>> findSimilarity(Long cognaId, String search, int maxResult, Double minScore) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        EmbeddingStore<TextSegment> es = getEmbeddingStore(cogna);

        EmbeddingModel em = getEmbeddingModel(cogna);
        EmbeddingSearchResult<TextSegment> matches;
        if (minScore != null) {
            matches = es.search(EmbeddingSearchRequest.builder().queryEmbedding(em.embed(search).content())
                    .maxResults(maxResult)
                    .minScore(minScore).build());
        } else {
            matches = es.search(EmbeddingSearchRequest.builder().queryEmbedding(em.embed(search).content())
                    .maxResults(maxResult).build());
        }

        return matches
                .matches()
                .stream()
                .map(match -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("score", match.score());
                    item.put("metadata", match.embedded().metadata().toMap());
                    item.put("embeddingId", match.embeddingId());
                    item.put("text", match.embedded().text());
                    return item;
                })
                .toList();
    }

    /* FOR LAMBDA */
    public List<String> classifyWithLlm(Long cognaId, Map<String, String> options, String what, String text, boolean multiple) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        return classifyWithLlm(cogna, options, what, text, multiple);
    }

    public List<String> classifyWithLlm(Cogna cogna, Map<String, String> options, String what, String text, boolean multiple) {
        TextProcessor textProcessor;
        if (textProcessorHolder.get(cogna.getId()) == null) {
            textProcessor = AiServices.create(TextProcessor.class, getChatModel(cogna, null));
            textProcessorHolder.put(cogna.getId(), textProcessor);
        } else {
            textProcessor = textProcessorHolder.get(cogna.getId());
        }

        String classificationMulti = multiple ? "If applicable, you can choose MULTIPLE from the following choices: " :
                "You must choose ONLY ONE from the following choices: ";

        String classification = options.keySet().stream().collect(Collectors.joining(", "));

        List<String> entryList = new ArrayList<>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null) {
                entryList.add(key + ": " + value);
            }
        }
        String classificationDesc = entryList.stream().collect(Collectors.joining("\n\n"));

        return textProcessor.textClassification(what, classification, classificationDesc, classificationMulti, text);
    }

    public Map<String, Object> classifyWithLlmSimpleOption(Cogna cogna, List<String> options, String what, String text) {

        List<String> categoryCode = classifyWithLlm(cogna,
                options.stream().collect(Collectors.toMap(o -> o, o -> o)),
                what,
                text,
                false);

        Map<String, Object> returnVal = new HashMap<>();

        if (categoryCode != null && categoryCode.size() > 0) {
            returnVal.put("category", categoryCode.get(0));
            returnVal.put("data", categoryCode.get(0));
        }

        return returnVal;
    }

    public Map<String, Object> classifyWithLlmLookup(Cogna cogna, Long lookupId, String what, String text, boolean multiple) {

        Map<String, LookupEntry> classificationMap;
        Map<String, String> classificationObj;
        try {
            List<LookupEntry> entryList = (List<LookupEntry>) lookupService.findAllEntry(lookupId, null, null, true, PageRequest.of(0, Integer.MAX_VALUE)).getOrDefault("content", List.of());

            classificationObj = entryList.stream()
                .collect(Collectors.toMap(LookupEntry::getCode,
                    e -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append(e.getName());

                        if (e.getExtra() != null && !e.getExtra().isEmpty()) {
                            sb.append("\nExtra: ").append(e.getExtra());
                        }

                        if (e.getData() != null && !e.getData().isEmpty()) {
                            sb.append("\nAdditional Data: ").append(e.getData());
                        }

                        return sb.toString();
                    }));

            classificationMap = entryList.stream()
                    .collect(Collectors.toMap(LookupEntry::getCode, entry -> entry));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<String> categoryCode = classifyWithLlm(cogna,
                classificationObj,
                what,
                text, multiple);

        Map<String, Object> returnVal = new HashMap<>();

        if (categoryCode != null && categoryCode.size() > 0) {

            final Map<String, LookupEntry> cMap = classificationMap;

            if (multiple) {
                List<LookupEntry> listData = new ArrayList<>();
                for (String c : categoryCode) {
                    if (cMap.get(c) != null) {
                        listData.add(cMap.get(c));
                    }
                }
                if (listData.size() > 0) {
                    returnVal.put("category", categoryCode);
                    returnVal.put("data", listData);
                }
            } else {
                if (cMap.get(categoryCode.get(0)) != null) {
                    returnVal.put("category", categoryCode.get(0));
                    returnVal.put("data", cMap.get(categoryCode.get(0)));
                }
            }
        }
        return returnVal;
    }

    /* FOR LAMBDA */
    public Map<String, Object> classifyWithEmbedding(Long cognaId, String text, Double minScore, boolean multiple) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        return classifyWithEmbedding(cogna, text, minScore, multiple);
    }

    public Map<String, Object> classifyWithEmbedding(Cogna cogna, String text, Double minScore, boolean multiple) {
        EmbeddingModel embeddingModel = getEmbeddingModel(cogna);
        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(cogna);

        Embedding queryEmbedding = embeddingModel.embed(text).content();

        int maxResult = multiple ? 5 : 1;
        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .minScore(minScore)
                .maxResults(maxResult)
                .build()).matches();

        Map<String, Object> returnVal = new HashMap<>();

        if (relevant.size() == 0) {
            if (multiple) {
                return Map.of("category", List.of(), "data", List.of(), "score", List.of());
            } else {
                return returnVal;
            }
        }


        if (multiple) {
            Map<String, Double> categoryScores = new LinkedHashMap<>(); // Maps category to score while maintaining order

            for (EmbeddingMatch<TextSegment> match : relevant) {
                String category = match.embedded().metadata().getString("category");
                Double score = match.score();
                if (category != null && score != null) {
                    if (!categoryScores.containsKey(category) || categoryScores.get(category) < score) {
                        // Only put the category and score if it's a new category or a better score for an existing category
                        categoryScores.put(category, score);
                    }
                    // Check if we have reached the maxResult number of distinct categories.
                    if (categoryScores.size() == maxResult) {
                        continue;
                    }
                }
            }

            // Split the categories and scores into separate lists if needed.
            List<String> categories = new ArrayList<>(categoryScores.keySet());
            List<Double> scores = new ArrayList<>(categoryScores.values());


            if (categories.size() > 0) {
                returnVal.put("category", categories);
                returnVal.put("data", categories);
            }
            if (scores.size() > 0) {
                returnVal.put("score", scores);
            }

        } else {

            EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

            if (embeddingMatch.embedded().metadata().getString("category") != null)
                returnVal.put("category", embeddingMatch.embedded().metadata().getString("category"));
            returnVal.put("data", embeddingMatch.embedded().metadata().getString("category"));
            if (embeddingMatch.score() != null)
                returnVal.put("score", embeddingMatch.score());

        }

        return returnVal;
    }

    /**
     * FOR LAMBDA
     **/
    public Map<String, Object> classify(Long cognaId, String text, Long lookupId, String what, Double minScore, boolean multiple) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(() -> new ResourceNotFoundException("Cogna", "id", cognaId));
        if (cogna.getData().at("/txtclsLlm").asBoolean(false)) {
            return classifyWithLlmLookup(cogna, lookupId, what, text, multiple);
        } else {
            return classifyWithEmbedding(cogna, text, minScore, multiple);
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Map<String, Object> classifyField(Long fieldId, String text) {
        Item item = itemRepository.findById(fieldId).orElseThrow();

        Cogna cogna = cognaRepository.findById(item.getX().at("/rtxtcls").asLong()).orElseThrow();
        boolean multiple = "checkboxOption".equals(item.getType()) ||
                ("select".equals(item.getType()) && "multiple".equals(item.getSubType()));

        if (cogna.getData().at("/txtclsLlm").asBoolean(false)) {
            String what = item.getLabel();
            Long lookupId = item.getDataSource();

            boolean isLookup = !"simpleOption".equals(item.getType());

            if (isLookup) {
                return classifyWithLlmLookup(cogna, lookupId, what, text, multiple);
            } else {
                List<String> options = Helper.parseCSV(item.getOptions());
                return classifyWithLlmSimpleOption(cogna, options, what, text);
            }

        } else {
            return classifyWithEmbedding(cogna, text, 0.8, multiple);
        }

    }

    public Map<String, Object> txtgenField(Long fieldId, String text, String action) {
        Item item = itemRepository.findById(fieldId).orElseThrow();

        Cogna cogna = cognaRepository.findById(item.getX().at("/rtxtgen").asLong()).orElseThrow();

        Map<String, Object> returnVal = new HashMap<>();

        List<String> links = Helper.extractURLFromText(text);

        boolean mmSupport = Optional.ofNullable(cogna.getMmSupport()).orElse(false);
        boolean enableExtract = cogna.getData().at("/txtextractOn").asBoolean(false);

        List<String> contents = new ArrayList<>();
        List<ImageContent> images = new ArrayList<>();

        for (String url : links) {
            if (mmSupport && isImageFromUrl(url)) {
                images.add(ImageContent.from(url));
            }

            if (enableExtract) {
                String extractedText = getTextFromRekaURL(url);
                if (extractedText != null && !extractedText.isEmpty()) {
                    contents.add(getTextFromRekaURL(url));
                }
            }
        }

        text = text + "\n\n" + String.join("\n\n", contents);


        returnVal.put("action", action);

        if ("summarize".equals(action)) {
            returnVal.put("data",
                    summarize(cogna.getId(), text, images, item.getX().at("/rtxtgenSummarizeN").asInt(5))
            );
        } else if ("translate".equals(action)) {
            returnVal.put("data",
                    translate(cogna.getId(), text, images, item.getX().at("/rtxtgenTranslateLang").asText("English"))
            );
        } else if ("generate".equals(action)) {
            returnVal.put("data",
                    generate(cogna.getId(), text, images, item.getX().at("/rtxtgenGenerateMsg").asText(""))
            );
        }
        return returnVal;
    }

    /**
     * FOR LAMBDA
     **/
    public List<JsonNode> extract(Long cognaId, Map obj) {
        return extract(cognaId, MAPPER.convertValue(obj, CognaService.ExtractObj.class));
    }

    /**
     * FOR LAMBDA
     **/
    public String generateImage(Long cognaId, String text) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        OpenAiImageModel model = new OpenAiImageModel.OpenAiImageModelBuilder()
                .modelName(Optional.ofNullable(cogna.getInferModelName()).orElse("dall-e-3"))
                .size(cogna.getData().at("/imgSize").asText("1024x1024"))
//                .quality(cogna.getData().at("/imgQuality").asText("standard"))
//                .style(cogna.getData().at("/imgStyle").asText("vivid"))
                .logRequests(true)
                .logResponses(true)
                .apiKey(cogna.getInferModelApiKey())
                .build();

        Response<Image> response = model.generate(text);

        String url = response.content().url().toString();

        if (cogna.getData().at("/genBucket").asBoolean(false)) {
            Long bucketId = cogna.getData().at("/genBucketId").asLong();
            if (bucketId != null) {
                EntryAttachment ea = bucketService.addUrlToBucket(bucketId, url, cogna.getApp().getId(), cogna.getEmail());
                url = IO_BASE_DOMAIN + "/api/entry/file/" + ea.getFileUrl();
            }
        }
        return url; // Donald Duck is here :)
    }

    public Map<String, Object> generateImageField(Long itemId, String text) {
        Item item = itemRepository.findById(itemId).orElseThrow();

        Cogna cogna = cognaRepository.findById(item.getX().at("/rimggen").asLong()).orElseThrow();

        OpenAiImageModel model = new OpenAiImageModel.OpenAiImageModelBuilder()
                .modelName(Optional.ofNullable(cogna.getInferModelName()).orElse("dall-e-3"))
                .size(cogna.getData().at("/imgSize").asText("1024x1024"))
                .logRequests(true)
                .logResponses(true)
                .apiKey(cogna.getInferModelApiKey())
                .build();

        Response<Image> response = model.generate(text);

        String url = response.content().url().toString();

        if (cogna.getData().at("/genBucket").asBoolean(false)) {
            Long bucketId = cogna.getData().at("/genBucketId").asLong();
            if (bucketId != null) {
                EntryAttachment ea = bucketService.addUrlToBucket(bucketId, url, cogna.getApp().getId(), cogna.getEmail());
                url = ea.getFileUrl();
            }
        }

        Map<String, Object> rval = new HashMap<>();

        rval.put("text", text);

        if (List.of("imagemulti", "othermulti").contains(item.getSubType())) {
            rval.put("data", List.of(url));
        } else {
            rval.put("data", url);
        }
        return rval;
    }

    public List<JsonNode> extract(Long cognaId, CognaService.ExtractObj extractObj) {

        if (extractObj == null || (extractObj.docList() == null && (extractObj.text() == null || extractObj.text().isBlank()))) {
            return List.of();
        }

        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(() -> new ResourceNotFoundException("Cogna", "id", cognaId));

        ChatModel model = getChatModel(cogna, "json_object");

        String jsonSchemaProps = cogna.getData()
                .at("/extractSchema")
                .asText();

        String jsonSchemaText = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": $props$,
                  "additionalProperties": false
                }
                """
                .replace("$props$", jsonSchemaProps);

        JsonRawSchema jsonRawSchema = JsonRawSchema.from(jsonSchemaText);

        final ResponseFormat responseFormat = ResponseFormat.builder()
                .type(JSON) // type can be either TEXT (default) or JSON
                .jsonSchema(JsonSchema.builder()
                        .name("Data") // OpenAI requires specifying the name for the schema
                        .rootElement(jsonRawSchema)
                        .build()) // for JSON type, you can specify either a JsonSchema or a String
//                .jsonSchema(JsonSchema.builder()
//                        .name("Data") // OpenAI requires specifying the name for the schema
//                        .rootElement(JsonSchemaConvertUtil.convertJsonSchema(jsonSchemaText))
//                        .build())
                .build();

        List<JsonNode> listData = new ArrayList<>();
        if (extractObj.docList() != null) {
            extractObj.docList().parallelStream().forEach(m -> {
                try {
                    String text = getTextFromRekaPath(cognaId, m, extractObj.fromCogna());

                    if (text != null && !text.isBlank()) {
                        List<ChatMessage> messages = Collections.singletonList(
                                new dev.langchain4j.data.message.UserMessage(text)
                        );

                        ChatRequest chatRequest = ChatRequest.builder()
                                .parameters(ChatRequestParameters.builder()
                                        .responseFormat(responseFormat).build())
//                                .responseFormat(responseFormat)
                                .messages(messages)
                                .build();

                        ChatResponse chatResponse = model.chat(chatRequest);

                        listData.add(MAPPER.readTree(
                                        chatResponse.aiMessage().text()
                                )
                        );
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }


        if (extractObj.text() != null && !extractObj.text().isBlank()) {
            try {
                List<ChatMessage> messages = Collections.singletonList(
                        new dev.langchain4j.data.message.UserMessage(extractObj.text())
                );

                ChatRequest chatRequest = ChatRequest.builder()
                        .parameters(ChatRequestParameters.builder()
                                .responseFormat(responseFormat).build())
//                        .responseFormat(responseFormat)
                        .messages(messages)
                        .build();

                ChatResponse chatResponse = model.chat(chatRequest);

                listData.add(MAPPER.readTree(
                                chatResponse.aiMessage().text()
                        )
                );

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return listData;
    }

    /**
     * public Map<String,Object> imgclsOld(Long cognaId, CognaService.ExtractObj extractObj) {
     * <p>
     * Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(()->new ResourceNotFoundException("Cogna","id", cognaId));
     * <p>
     * if (!(extractObj!=null || extractObj.docList()!=null)){
     * return Map.of();
     * }
     * <p>
     * Map<String,Object> listData = new HashMap<>();
     * if (extractObj.docList() != null) {
     * extractObj.docList().parallelStream().forEach(m -> {
     * try {
     * String filePath;
     * String fileDir;
     * if (extractObj.fromCogna()) {
     * filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/" + m;
     * fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId;
     * } else {
     * EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(m);
     * if (entryAttachment != null && entryAttachment.getBucketId() != null) {
     * filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + entryAttachment.getBucketId() + "/" + m;
     * fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + entryAttachment.getBucketId();
     * } else {
     * filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/"+ m;
     * fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment";
     * }
     * }
     * listData.put(m,
     * detectImgOld(cognaId,fileDir, m)
     * );
     * //                    if (cogna.getInferModelName().contains("resnet")|| cogna.getInferModelName().contains("mobilenet")){
     * //                        listData.put(m,
     * //                                classifyImg(cognaId,fileDir, m)
     * //                        );
     * //                    }else if (cogna.getInferModelName().contains("yolo")){
     * //                        listData.put(m,
     * //                                detectImg(cognaId,fileDir, m)
     * //                        );
     * //                    }
     * <p>
     * } catch (Exception e) {
     * throw new RuntimeException(e);
     * }
     * });
     * }
     * <p>
     * return listData;
     * }
     **/

    public Map<String, List<ImagePredict>> imgcls(Long cognaId, CognaService.ExtractObj extractObj) {

        cognaRepository.findById(cognaId).orElseThrow(() -> new ResourceNotFoundException("Cogna", "id", cognaId));

        if (!(extractObj != null || extractObj.docList() != null)) {
            return Map.of();
        }

        Map<String, List<ImagePredict>> listData = new ConcurrentHashMap<>();

        extractObj.docList().parallelStream().forEach(fileName -> {
            try {
                String filePath;
                String fileDir;
                if (extractObj.fromCogna()) {
                    filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/" + fileName;
                    fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId;
                } else {
                    EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(fileName);
                    if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                        filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + entryAttachment.getBucketId() + "/" + fileName;
                        fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + entryAttachment.getBucketId();
                    } else {
                        filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/" + fileName;
                        fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment";
                    }
                }
//                    DetectedObjects dob = detectImg(cognaId,fileDir,m);
//                    List<ImagePredict> lip = dob.item(0).
                listData.put(fileName,
                        detectImg(cognaId, fileDir, fileName)
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return listData;
    }

    /**
     * FOR LAMBDA
     **/
    public String translate(Long cognaId, String text, List<ImageContent> images, String language) {
        TextProcessor textProcessor;
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        if (textProcessorHolder.get(cognaId) == null) {
            textProcessor = AiServices
                    .builder(TextProcessor.class)
                    .chatModel(getChatModel(cogna, null))
                    .build();

            textProcessorHolder.put(cognaId, textProcessor);
        } else {
            textProcessor = textProcessorHolder.get(cognaId);
        }

        return textProcessor.translate(text, images, language);
    }

    /**
     * FOR LAMBDA
     **/
    public List<String> summarize(Long cognaId, String text, List<ImageContent> images, int pointCount) {
        TextProcessor textProcessor;
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        if (textProcessorHolder.get(cognaId) == null) {
            textProcessor = AiServices.create(TextProcessor.class, getChatModel(cogna, null));
            textProcessorHolder.put(cognaId, textProcessor);
        } else {
            textProcessor = textProcessorHolder.get(cognaId);
        }
        return textProcessor.summarize(text, images, pointCount);
    }

    /**
     * FOR LAMBDA
     **/
    public String generate(Long cognaId, String text, List<ImageContent> images, String instruction) {
        TextProcessor textProcessor;
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        if (textProcessorHolder.get(cognaId) == null) {
            textProcessor = AiServices.create(TextProcessor.class, getChatModel(cogna, null));
            textProcessorHolder.put(cognaId, textProcessor);
        } else {
            textProcessor = textProcessorHolder.get(cognaId);
        }
        return textProcessor.generate(text, images, instruction);
    }

    /**
     * FOR LAMBDA
     **/
    public String prompt(Long cognaId, Map obj, String email) {
        return prompt(email, cognaId, MAPPER.convertValue(obj, CognaService.PromptObj.class));
    }

    record AssistantConfig(Cogna cogna,ChatMemory chatMemory,EmbeddingStore<TextSegment> embeddingStore,EmbeddingModel embeddingModel,
                           RetrievalAugmentor retrievalAugmentor,Map<ToolSpecification, ToolExecutor> tools,List<McpClient> mcpClients) {}

    public Assistant getAssistant(Cogna cogna, String email) {

        return assistantHolder
            .computeIfAbsent(cogna.getId(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(email, e -> {

                ChatMemory memory = getChatMemory(cogna, email);
                AssistantConfig cfg = buildAssistantConfig(cogna, memory);

                String responseFormat =
                        cogna.getData().at("/jsonOutput").asBoolean()
                                ? "json_schema"
                                : null;

                AiServices<Assistant> assistantBuilder =
                        AiServices.builder(Assistant.class)
                                .chatModel(getChatModel(cogna, responseFormat))
                                .chatMemoryProvider(id -> cfg.chatMemory())
                                .retrievalAugmentor(cfg.retrievalAugmentor());

                if (!cfg.tools().isEmpty()) {
                    assistantBuilder.toolProvider(req -> new ToolProviderResult(cfg.tools()));
                }

                if (!cfg.mcpClients().isEmpty()) {
                    assistantBuilder.toolProvider(
                            McpToolProvider.builder()
                                    .mcpClients(cfg.mcpClients())
                                    .build()
                    );
                }

                return assistantBuilder.build();
            });
    }

    public SubAgent getAgent(Cogna cogna, Cogna masterCogna, String email) {

        return agentHolder.computeIfAbsent(cogna.getId(), k -> {

            ChatMemory memory = getChatMemory(masterCogna, email);
            AssistantConfig cfg = buildAssistantConfig(cogna, memory);

            String responseFormat = cogna.getData().at("/jsonOutput").asBoolean() ? "json_schema" : null;

            AgentBuilder<SubAgent> assistantBuilder =
                AgenticServices.agentBuilder(SubAgent.class)
                    .chatModel(getChatModel(cogna, responseFormat))
                    .chatMemoryProvider(id -> cfg.chatMemory())
                    .retrievalAugmentor(cfg.retrievalAugmentor())
                    .outputKey("response");

            if (!cfg.tools().isEmpty()) {
                assistantBuilder.toolProvider(req -> new ToolProviderResult(cfg.tools()));
            }

            if (!cfg.mcpClients().isEmpty()) {
                assistantBuilder.toolProvider(
                        McpToolProvider.builder()
                                .mcpClients(cfg.mcpClients())
                                .build()
                );
            }

            return assistantBuilder.build();
        });
    }

    public String masterPrompt(Long cognaId, String userMessage, List<Content> contentList, String systemMessage, String email) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();

//        ChatMemory thisChatMemory = getChatMemory(cogna, email);

        List<SubAgent> subAssistants = new ArrayList<>();
        for (CognaSub sub : cogna.getSubs()) {
            if (sub.isEnabled()) {
                Cogna subCogna = cognaRepository.findById(sub.getSubId()).orElseThrow();
                subAssistants.add(getAgent(subCogna, cogna, email));
            }
        }

        MasterAgent agent = AgenticServices
                .parallelBuilder(MasterAgent.class)
                .subAgents(subAssistants.toArray(new SubAgent[0]))
                .executor(executor)
                .outputKey("response")
                .build();

        return agent.chat(userMessage, contentList, systemMessage);
    }

    record PromptContext(Cogna cogna,String prompt,String systemMessage,List<Content> contentList,String email) {}
    private PromptContext buildPromptContext(String email, Long cognaId, CognaService.PromptObj promptObj) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();

        Map<String, Object> dataMap = new HashMap<>();
        if (promptObj != null && promptObj.param() != null) {
            dataMap.put("param", promptObj.param());
        }

        userRepository.findFirstByEmailAndAppId(email, cogna.getApp().getId())
                .ifPresentOrElse(user -> {
                    dataMap.put("user", MAPPER.convertValue(user, Map.class));
                }, () -> {
                    if (email != null) {
                        dataMap.put("user", Map.of("email", email, "name", email));
                    }
                });

        String systemMessage = Helper.compileTpl(
                Optional.ofNullable(cogna.getSystemMessage())
                        .orElse("Your name is Cogna"),
                dataMap
        );

        String prompt = promptObj.prompt();
        List<Content> contentList = new ArrayList<>();

        // JSON output constraint
        if (cogna.getData().at("/jsonOutput").asBoolean()) {
            systemMessage += "\n\nCRITICAL INSTRUCTION:\n"
                    + "  - You must ONLY output a valid JSON object\n"
                    + "  - Do not include any explanatory text before or after the JSON\n"
                    + "  - Do not use markdown code blocks\n"
                    + "  - Do not include any additional formatting\n"
                    + "  - If you cannot provide accurate information, still return a valid JSON\n\n"
                    + "STRICT RESPONSE FORMAT:\n"
                    + "Return ONLY a JSON object with this exact structure - no additional text or explanations:\n"
                    + cogna.getData().at("/extractSchema").asText();
        }

        // Multimodal
        if (promptObj.fileList() != null && !promptObj.fileList().isEmpty()) {
            if (!StringUtils.hasText(prompt)) {
                prompt = "Describe the image - no additional text or explanations";
            }

            boolean showScore = cogna.getData().at("/imgclsShowScore").asBoolean(false);

            for (String file : promptObj.fileList()) {
                Path filePath = getPath(cognaId, file, promptObj.fromCogna());
                String fileUrl = getUrl(cognaId, file, promptObj.fromCogna());

                if (isImage(cognaId, file, promptObj.fromCogna())) {
                    if (Boolean.TRUE.equals(cogna.getMmSupport())) {
                        contentList.add(ImageContent.from(fileUrl));
                    }

                    if (cogna.getData().at("/imgclsOn").asBoolean(false)) {
                        try {
                            List<ImagePredict> prediction =
                                    classifyImg(
                                            cogna.getData().at("/imgclsCogna").asLong(),
                                            filePath.getParent().toString(),
                                            file
                                    );

                            if (!prediction.isEmpty()) {
                                String text = prediction.stream()
                                        .map(p -> p.desc() + (showScore ? " (score: " + p.score() + ")" : ""))
                                        .collect(Collectors.joining("\n"));

                                contentList.add(TextContent.from("Image classified as : " + text));
                            }
                        } catch (Exception e) {
                            logger.error("Error classifying image", e);
                        }
                    }
                }

                if (cogna.getData().at("/txtextractOn").asBoolean(false)) {
                    String text = getTextFromRekaPath(cognaId, file, true);
                    if (StringUtils.hasText(text)) {
                        contentList.add(TextContent.from("Text in the attachment: " + text));
                    }
                }
            }
        }

        if (cogna.getPostMessage() != null) {
            prompt += "\n\n" + cogna.getPostMessage();
        }

        return new PromptContext(cogna, prompt, systemMessage, contentList, email);
    }

    public String prompt(String email, Long cognaId, CognaService.PromptObj promptObj) {
        PromptContext ctx = buildPromptContext(email, cognaId, promptObj);

        if ("master".equals(ctx.cogna().getType())) {
            return masterPrompt(
                    cognaId,
                    ctx.prompt(),
                    ctx.contentList(),
                    ctx.systemMessage(),
                    email
            );
        }

        Assistant assistant = getAssistant(ctx.cogna(), email);
        return assistant.chat(
                ctx.prompt(),
                ctx.contentList(),
                ctx.systemMessage()
        );

    }

    private Map<ToolSpecification, ToolExecutor> buildTools(Cogna cogna) {
        Map<ToolSpecification, ToolExecutor> toolMap = new HashMap<>();

        if (cogna.getData().at("/imggenOn").asBoolean(false)) {
            toolMap.put(
                ToolSpecification.builder()
                        .name("generate_image")
                        .description("Generate image from the specified text")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("text", "Description of the image")
                                .build())
                        .build(),
                (req, memId) -> {
                    try {
                        Map<String, Object> args =
                                MAPPER.readValue(req.arguments(), Map.class);
                        return generateImage(
                                cogna.getData().at("/imggenCogna").asLong(),
                                args.get("text").toString()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        }

        if (cogna.getTools().size() > 0) {
            UserPrincipal up = null;
            try {
                up = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            } catch (Exception ignored) { }

            final UserPrincipal userPrincipal = up;

            for (CognaTool tool : cogna.getTools()) {
                if (!tool.isEnabled()) continue;

                JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
                List<String> required = new ArrayList<>();

                if (tool.getParams() != null) {
                    for (JsonNode jsonNode : tool.getParams()) {
                        schemaBuilder.addStringProperty(
                                jsonNode.at("/key").asText(),
                                jsonNode.at("/description").asText()
                        );
                        if (jsonNode.at("/required").asBoolean(true)) {
                            required.add(jsonNode.at("/key").asText());
                        }
                    }
                }
                schemaBuilder.required(required);

                ToolSpecification toolSpecification = ToolSpecification.builder()
                        .name(tool.getName())
                        .description(tool.getDescription())
                        .parameters(schemaBuilder.build())
                        .build();

                ToolExecutor toolExecutor = (req, memId) -> {
                    try {
                        Map<String, Object> arguments = MAPPER.readValue(req.arguments(), Map.class);

                        Map<String, Object> executed = lambdaService.execLambda(tool.getLambdaId(), arguments, null, null, null, userPrincipal);

                        if (executed == null) return "Tool returned no response";

                        // Only get result from print value
                        return Boolean.parseBoolean(executed.get("success") + "")
                                ? String.valueOf(executed.get("print"))
                                : String.valueOf(executed.get("message"));

                    } catch (Exception e) {
                        logger.error("Tool execution failed", e);
                        return "Tool execution failed:" + e;
                    }
                };

                toolMap.put(toolSpecification, toolExecutor);
            }
        }

        return toolMap;
    }

    private AssistantConfig buildAssistantConfig(Cogna cogna, ChatMemory chatMemory) {
        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(cogna);
        EmbeddingModel embeddingModel = getEmbeddingModel(cogna);

        EmbeddingStoreContentRetriever.EmbeddingStoreContentRetrieverBuilder esrb = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(Optional.ofNullable(cogna.getEmbedMaxResult()).orElse(5));

        if (cogna.getEmbedMinScore() != null) {
            esrb.minScore(cogna.getEmbedMinScore());
        }

        RetrievalAugmentor retrievalAugmentor = switch (Optional.ofNullable(cogna.getAugmentor()).orElse("")) {
            case "compressor" -> getQueryCompressorAugmentor(cogna, esrb.build(), getChatModel(cogna, null));
            case "rerank" ->
                    getRerankAugmentor(cogna,esrb.build(),cogna.getData().at("/cohereApiKey").asText(),cogna.getData().at("/reRankMinScore").asDouble());
            default -> getDefaultAugmentor(cogna, esrb.build());
        };

        Map<ToolSpecification, ToolExecutor> tools = buildTools(cogna);
        List<McpClient> mcpClients = buildMcpClients(cogna);

        return new AssistantConfig(
                cogna,
                chatMemory,
                embeddingStore,
                embeddingModel,
                retrievalAugmentor,
                tools,
                mcpClients
        );
    }

    // Store MCPs per assistant for cleanup
    private final Map<Long, List<McpClient>> mcpClientsByCognaId = new ConcurrentHashMap<>();

    private List<McpClient> buildMcpClients(Cogna cogna) {
        List<McpClient> mcpClientList = new ArrayList<>();

        for (CognaMcp mcp : cogna.getMcps()) {
            if (!mcp.isEnabled()) continue;

            try {
                McpTransport transport = new StreamableHttpMcpTransport.Builder()
                        .url(mcp.getUrl())
                        .timeout(Duration.ofSeconds(mcp.getTimeout()))
                        .logRequests(true)
                        .logResponses(true)
                        .build();

                mcpClientList.add(new DefaultMcpClient.Builder()
                        .key(mcp.getName())
                        .transport(transport)
                        .build());
            } catch (Exception e) {
                logger.error("MCP error: {}", mcp.getName(), e);
            }
        }

        if (mcpClientList.size() > 0) {
            mcpClientsByCognaId.put(cogna.getId(), mcpClientList);
        }
        return mcpClientList;
    }

    public StreamingAssistant getStreamableAssistant(Cogna cogna, String email) {

        return streamAssistantHolder
            .computeIfAbsent(cogna.getId(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(email, e -> {

                ChatMemory memory = getChatMemory(cogna, email);
                AssistantConfig cfg = buildAssistantConfig(cogna, memory);

                AiServices<StreamingAssistant> assistantBuilder =
                        AiServices.builder(StreamingAssistant.class)
                                .streamingChatModel(getStreamingChatModel(cogna))
                                .chatMemory(cfg.chatMemory())
                                .retrievalAugmentor(cfg.retrievalAugmentor());

                if (!cfg.tools().isEmpty()) {
                    assistantBuilder.toolProvider(req -> new ToolProviderResult(cfg.tools()));
                }

                if (!cfg.mcpClients().isEmpty()) {
                    assistantBuilder.toolProvider(
                            McpToolProvider.builder()
                                    .mcpClients(cfg.mcpClients())
                                    .build()
                    );
                }

                return assistantBuilder.build();
            });
    }

    // ONLY SUPPORTED BY OPEN_AI WITH API KEY OR GEMINI PRO
    public TokenStream promptStream(String email, Long cognaId, CognaService.PromptObj promptObj) {
        PromptContext ctx = buildPromptContext(email, cognaId, promptObj);

        StreamingAssistant assistant =
                getStreamableAssistant(ctx.cogna(), email);

        return assistant.chat(
                ctx.prompt(),
                ctx.contentList(),
                ctx.systemMessage()
        );
    }

    public Map<String, Object> clearMemoryByIdAndEmail(Long cognaId, String email) {
        if (chatMemoryMap.get(cognaId) != null) {
            chatMemoryMap.get(cognaId).invalidate(email);
        }

        assistantHolder.remove(cognaId);

        agentHolder.remove(cognaId);

        streamAssistantHolder.remove(cognaId);

        return Map.of("success", true);
    }

    public Map<String, Object> clearMemoryById(Long cognaId) {
        Cache<String, ChatMemory> chatMemory = chatMemoryMap.remove(cognaId);
        if (chatMemory != null) {
            chatMemory.invalidateAll();
        }
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> clearDb(Long cognaId) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(cogna);

        if (MILVUS.equals(cogna.getVectorStoreType())) {
            embeddingStore.removeAll();
        }
        if (CHROMADB.equals(cogna.getVectorStoreType())) {
            embeddingStore.removeAll();
        }
        if (INMEMORY.equals(cogna.getVectorStoreType())) {
            File inMemoryStore = new File(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-" + cogna.getId() + ".store");
            if (inMemoryStore.isFile()) {
                inMemoryStore.delete();
            }
            embeddingStore.removeAll();
            storeHolder.remove(cognaId);
        }
        reinitCogna(cognaId);

        for (CognaSource s : cogna.getSources()) {
            s.setLastIngest(null);
        }
        cognaRepository.save(cogna);

        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> clearDbBySourceId(Long sourceId) {
        CognaSource cognaSource = cognaSourceRepository.findById(sourceId).orElseThrow();
        Cogna cogna = cognaSource.getCogna();
        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(cogna);

        if (MILVUS.equals(cogna.getVectorStoreType())) {
            Filter filter = metadataKey("source_id").isEqualTo(sourceId);
            embeddingStore.removeAll(filter);
        }
        if (CHROMADB.equals(cogna.getVectorStoreType())) {
            Filter filter = metadataKey("source_id").isEqualTo(sourceId);
            embeddingStore.removeAll(filter);
        }
        if (INMEMORY.equals(cogna.getVectorStoreType())) {
            File inMemoryStore = new File(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-" + cogna.getId() + ".store");
            if (inMemoryStore.isFile()) {
                inMemoryStore.delete();
            }
            Filter filter = metadataKey("source_id").isEqualTo(sourceId);
            embeddingStore.removeAll(filter);
            storeHolder.remove(cogna.getId());
        }
        reinitCogna(cogna.getId());

        cognaSource.setLastIngest(null);
        cognaSourceRepository.save(cognaSource);

        return Map.of("success", true);
    }

    public Map<String, Object> reinitCognaAndChatHistory(Long cognaId) {
        assistantHolder.remove(cognaId);
        agentHolder.remove(cognaId);
        streamAssistantHolder.remove(cognaId);
        storeHolder.remove(cognaId);
        Cache<String, ChatMemory> chatMemory = chatMemoryMap.remove(cognaId);
        if (chatMemory != null) {
            chatMemory.invalidateAll();
        }
        textProcessorHolder.remove(cognaId);
        return Map.of("success", true);
    }

    public Map<String, Object> reinitCogna(Long cognaId) {
        assistantHolder.remove(cognaId);
        agentHolder.remove(cognaId);
        streamAssistantHolder.remove(cognaId);
        storeHolder.remove(cognaId);
        textProcessorHolder.remove(cognaId);
//        chatMemoryMap.remove(cognaId);
        return Map.of("success", true);
    }

    public Map<String, Object> clearAllMemory() {
        for (Cache<String, ChatMemory> stringChatMemoryCache : chatMemoryMap.values()) {
            stringChatMemoryCache.invalidateAll();
        }
        chatMemoryMap.clear();
        return Map.of("success", true);
    }

    // transactional needed for Stream dataset

    public Map<Long, Map> ingest(Long cognaId) {

        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();

        Map<Long, Map> data = new HashMap<>();
        for (CognaSource s : cogna.getSources()) {
            data.put(s.getId(), ingestSource(s));
        }

        return data;
    }

    public void persistInMemoryVectorStore(Cogna cogna) {
        if (INMEMORY.equals(cogna.getVectorStoreType())) {
            File inMemoryStoreDir = new File(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store");
            inMemoryStoreDir.mkdirs();
            InMemoryEmbeddingStore<TextSegment> store = (InMemoryEmbeddingStore<TextSegment>) getEmbeddingStore(cogna);
            store.serializeToFile(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-" + cogna.getId() + ".store");
        }
    }

    @Transactional
    public Map<String, Object> ingestSource(CognaSource cognaSrc) {

        Cogna cogna = cognaSrc.getCogna();

        long ingestStart = System.currentTimeMillis();

        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(cogna);

        EmbeddingModel embeddingModel = getEmbeddingModel(cogna);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentTransformer(document -> {
                    if (document.metadata().getString("file_name") != null)
                        document.metadata().put("web_url", IO_BASE_DOMAIN + "/api/entry/file/" + document.metadata().getString("file_name"));

                    return document;
                })
                .documentSplitter(DocumentSplitters.recursive(
                        Optional.ofNullable(cogna.getChunkLength()).orElse(100),
                        Optional.ofNullable(cogna.getChunkOverlap()).orElse(10)))
                .textSegmentTransformer(textSegment -> {
                            if (textSegment.metadata().getString("file_name") != null)
                                textSegment.metadata().put("web_url", IO_BASE_DOMAIN + "/api/entry/file/" + textSegment.metadata().getString("file_name"));

                            return TextSegment.from(
                                    textSegment.text(),
                                    textSegment.metadata());
                        }
                )
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        AtomicInteger docCount = new AtomicInteger();

        if ("bucket".equals(cognaSrc.getType())) {
            Page<EntryAttachment> attachmentPage = entryAttachmentRepository.findByBucketId(cognaSrc.getSrcId(), "%", PageRequest.of(0, 999));

            try {
                // --- Prepare directories & paths ---
                Path dirPath = Paths.get(Constant.UPLOAD_ROOT_DIR, "attachment", "cogna-" + cogna.getId());
                Files.createDirectories(dirPath);

                Path bucketTxtPath = dirPath.resolve("bucket-" + cognaSrc.getSrcId() + ".txt");

                if (cognaSrc.getLastIngest() == null) { // mn xpernah knak ingest, make sure null, so, mn clear db + ingest date == null
                    Files.deleteIfExists(bucketTxtPath);
                }

                try (FileWriter fw = new FileWriter(bucketTxtPath.toFile(), true)) {

                    attachmentPage.forEach(at -> {

                        // ingest only when source not yet ingested OR attachment is uploaded after last ingest
                        // problem cara tok, plain text nya x menggambarkan embeddings sbb partial jk\
                        // ataupun, just append without reset
                        boolean shouldIngest = cognaSrc.getLastIngest() == null || at.getTimestamp().after(cognaSrc.getLastIngest());

                        if (!shouldIngest) return;

                        try {
                            Path filePath = Paths.get(
                                    Constant.UPLOAD_ROOT_DIR,
                                    "attachment",
                                    "bucket-" + cognaSrc.getSrcId(),
                                    at.getFileUrl()
                            );

                            Document doc;
                            if (at.getFileType().contains("image")) {
                                String text = Helper.ocr(filePath.toString(), "eng");
                                doc = Document.from(text);
                            } else {
                                doc = loadDocument(filePath, new ApacheTikaDocumentParser());
                            }

                            logger.info("ingest: " + doc.text());
                            if (doc != null) {
                                doc.metadata().put("source_id", cognaSrc.getId());
                                doc.metadata().put("source_url", IO_BASE_DOMAIN + "/api/entry/file/" + at.getFileUrl());

                                ingestor.ingest(doc);
                                docCount.getAndIncrement();

                                fw.write(doc.text() + "\n\n");
                            }
                        } catch (Exception e) {
                            logger.error("Error ingest (" + cognaSrc.getName() + "):" + e.getMessage());
                        }
                    });
                }

                updateCognaSourceLastIngest(cognaSrc.getId());

                persistInMemoryVectorStore(cogna);

            } catch (IOException e) {
                logger.error("Error ingest (" + cognaSrc.getName() + "):" + e.getMessage());
            }
        }
        if ("dataset".equals(cognaSrc.getType())) {
            try {
                // --- Prepare directory ---
                Path baseDir = Paths.get(Constant.UPLOAD_ROOT_DIR, "attachment", "cogna-" + cogna.getId());
                Files.createDirectories(baseDir);

                Path outputPath = baseDir.resolve("dataset-" + cognaSrc.getSrcId() + ".txt");

                Files.deleteIfExists(outputPath);

                self.ingestDataset(embeddingStore, embeddingModel, outputPath, cognaSrc, cognaSrc.getSrcId(), "%", null, null, null, null, null);

                docCount.getAndIncrement();

                updateCognaSourceLastIngest(cognaSrc.getId());

                persistInMemoryVectorStore(cogna);

            } catch (IOException e) {
                logger.error("Error ingest (" + cognaSrc.getName() + "):" + e.getMessage());
            }
        }
        if ("url".equals(cognaSrc.getType())) {

            try {
                // --- Prepare directory ---
                Path baseDir = Paths.get(Constant.UPLOAD_ROOT_DIR, "attachment", "cogna-" + cogna.getId());
                Files.createDirectories(baseDir);

                Path outputPath = baseDir.resolve("web-" + cognaSrc.getId() + ".txt");

                Document htmlDoc = UrlDocumentLoader.load(cognaSrc.getSrcUrl(), new TextDocumentParser());
//                HtmlTextExtractor transformer = new HtmlTextExtractor(null, null, true);
                HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer();
                Document doc = transformer.transform(htmlDoc);

                doc.metadata().put("source_id", cognaSrc.getId());
                doc.metadata().put("source_url", cognaSrc.getSrcUrl());

                Files.writeString(outputPath, doc.text());

                ingestor.ingest(doc);
                docCount.getAndIncrement();

                updateCognaSourceLastIngest(cognaSrc.getId());

                persistInMemoryVectorStore(cogna);
            } catch (IOException e) {
                logger.error("Error ingest (" + cognaSrc.getName() + "):" + e.getMessage());
            }

        }

        long ingestEnd = System.currentTimeMillis();

        return Map.of("success", true, "docCount", docCount.get(), "timeMilis", (ingestEnd - ingestStart));

    }

    //    @Transactional
    public void updateCognaSourceLastIngest(Long cognaSrcId) {
        cognaSourceRepository.updateLastIngest(cognaSrcId, new Date());
    }

    public RetrievalAugmentor getDefaultAugmentor(Cogna cogna, ContentRetriever contentRetriever) {
        DefaultContentInjector.DefaultContentInjectorBuilder contentInjector = DefaultContentInjector.builder();
        // DEFAULT AUGMENTOR IALAH EMPTY AUGMENTOR WITH METADATA
        // Each retrieved segment should include "file_name" and "index" metadata values in the prompt
        if (cogna.getData().at("/withMetadata").asBoolean(false)) {
            contentInjector.metadataKeysToInclude(
                    asList(cogna.getData().at("/metadataKeys").asText("").split(","))
            );
        }

        return DefaultRetrievalAugmentor.builder()
//                .queryTransformer(queryTransformer)
                .contentInjector(contentInjector.build())
                .contentRetriever(contentRetriever)
                .build();
    }

    public RetrievalAugmentor getQueryCompressorAugmentor(Cogna cogna, ContentRetriever contentRetriever, ChatModel chatModel) {
        DefaultContentInjector.DefaultContentInjectorBuilder contentInjector = DefaultContentInjector.builder();
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel);
        // The RetrievalAugmentor serves as the entry point into the RAG flow in LangChain4j.
        // It can be configured to customize the RAG behavior according to your requirements.
        // In subsequent examples, we will explore more customizations.

        if (cogna.getData().at("/withMetadata").asBoolean(false)) {
            contentInjector.metadataKeysToInclude(
                    asList(cogna.getData().at("/metadataKeys").asText("").split(","))
            );
        }

        return DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector.build())
                .build();
    }

    public RetrievalAugmentor getRerankAugmentor(Cogna cogna, ContentRetriever contentRetriever, String cohereApiKey, Double reRankMinScore) {

        DefaultContentInjector.DefaultContentInjectorBuilder contentInjector = DefaultContentInjector.builder();
        // To register and get a free API key for Cohere, please visit the following link:
        // https://dashboard.cohere.com/welcome/register
        ScoringModel scoringModel = CohereScoringModel.builder()
                .apiKey(cohereApiKey)
                .modelName("rerank-multilingual-v3.0")
                .build();

        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                .minScore(reRankMinScore) // we want to present the LLM with only the truly relevant segments for the user's query
                .build();

        if (cogna.getData().at("/withMetadata").asBoolean(false)) {
            contentInjector.metadataKeysToInclude(
                    asList(cogna.getData().at("/metadataKeys").asText("").split(","))
            );
        }

        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(contentAggregator)
                .contentInjector(contentInjector.build())
                .build();
    }

//    public RetrievalAugmentor getMetadataAugmentor(ContentRetriever contentRetriever, String includeKeys) {
//
//        // Each retrieved segment should include "file_name" and "index" metadata values in the prompt
//        ContentInjector contentInjector = DefaultContentInjector.builder()
//                // .promptTemplate(...) // Formatting can also be changed
//                .metadataKeysToInclude(asList(includeKeys.split(",")))
//                .build();
//
//        return DefaultRetrievalAugmentor.builder()
//                .contentRetriever(contentRetriever)
//                .contentInjector(contentInjector)
//                .build();
//    }

    @Transactional(readOnly = true)
    public Map<String, Object> ingestDataset(EmbeddingStore<TextSegment> store,
                                             EmbeddingModel embeddingModel,
                                             Path txtPath, CognaSource cognaSrc, Long datasetId, String searchText,
                                             String email, Map filters, String cond, List<Long> ids, HttpServletRequest req) throws IOException {

        Map<String, Object> data = new HashMap<>();

        final Dataset dataset = datasetRepository.findById(datasetId).orElseThrow();
        final Form form = dataset.getForm();
        final App app = dataset.getApp();

        AtomicInteger index = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();

        // Use modern NIO writer + try-with-resources
        try (BufferedWriter writer = Files.newBufferedWriter(txtPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
             Stream<Entry> entryStream =
                     entryService.findListByDatasetStream(datasetId, searchText, email, filters, null, null, ids, req)) {

            Filter filter = metadataKey("source_id").isEqualTo(String.valueOf(cognaSrc.getId()));

            // have to clear first, because cannot update
            store.removeAll(filter);

            entryStream.forEach(entry -> {

                try {

                    this.entityManager.detach(entry);

                    String sentence = toTextSentence(cognaSrc.getSentenceTpl(), entry, form, app);
                    String category = (cognaSrc.getCategoryTpl() != null)
                            ? toTextSentence(cognaSrc.getCategoryTpl(), entry, form, app) : "";

                    String cleanSentence = Helper.html2text(sentence);

                    TextSegment segment1 = TextSegment.from(cleanSentence, Metadata.from(
                            Map.of(
                                    "category", category,
                                    "dataset", String.valueOf(datasetId),
                                    "source_id", String.valueOf(cognaSrc.getId()),
                                    "source_url", IO_BASE_DOMAIN + "/api/entry/view?entryId=" + entry.getId()
                            )
                    ));

                    Embedding embedding1 = embeddingModel.embed(segment1).content();

                    store.add(embedding1, segment1);

                    writer.write(cleanSentence);
                    writer.newLine();

                    index.getAndIncrement();


                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    total.incrementAndGet();
                }
            });
        }

        data.put("totalCount", total.get());
        data.put("totalSegment", index.get());
        data.put("success", total.get() == index.get());
        data.put("partial", total.get() > index.get());
        return data;

    }

    public String toTextSentence(String sentenceTpl, Entry entry, Form form, App app) {
        if (sentenceTpl == null) {
            return "";
        }

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("_", entry);
        Map<String, Object> result = MAPPER.convertValue(entry.getData(), Map.class);
        Map<String, Object> prev = MAPPER.convertValue(entry.getPrev(), Map.class);

        String url = "https://" + app.getAppPath() + "." + Constant.UI_BASE_DOMAIN + "/#";
        dataMap.put("uiUri", url);
        dataMap.put("viewUri", url + "/form/" + form.getId() + "/view?entryId=" + entry.getId());
        dataMap.put("editUri", url + "/form/" + form.getId() + "/edit?entryId=" + entry.getId());

        if (result != null) {
            dataMap.put("code", result.get("$code"));
            dataMap.put("id", result.get("$id"));
            dataMap.put("counter", result.get("$counter"));
        }

        if (prev != null) {
            dataMap.put("prev_code", prev.get("$code"));
            dataMap.put("prev_id", prev.get("$id"));
            dataMap.put("prev_counter", prev.get("$counter"));
        }

        dataMap.put("data", result);
        dataMap.put("prev", prev);

        return Helper.compileTpl(Optional.ofNullable(sentenceTpl).orElse(""), dataMap);

    }

    @Scheduled(cron = "0 0/10 * * * ?") //0 */1 * * * *
    public Map<String, Object> runSchedule() {

        if (!schedulerEnabled) {
            logger.info("Scheduler disabled - skipping scheduled cogna ingestion");
            return null;
        }

        Calendar now = Calendar.getInstance();

        String clock = String.format("%02d%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        int day = now.get(Calendar.DAY_OF_WEEK); // sun=1, mon=2, tues=3,wed=4,thur=5,fri=6,sat=7
        int date = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH); // 0-based month, ie: Jan=0, Feb=1, March=2

        for (CognaSource s : cognaSourceRepository.findScheduledByClock(clock)) {
            if ("daily".equals(s.getFreq()) ||
                    ("weekly".equals(s.getFreq()) && s.getDayOfWeek() == day) ||
                    ("monthly".equals(s.getFreq()) && s.getDayOfMonth() == date) ||
                    ("yearly".equals(s.getFreq()) && s.getMonthOfYear() == month && s.getDayOfMonth() == date)
            ) {
                try {
                    long start = System.currentTimeMillis();
                    self.ingestSource(s);
                    long end = System.currentTimeMillis();
                    logger.info("Duration ingest (" + s.getName() + "):" + (end - start));
                } catch (Exception e) {
                    logger.error("ERROR executing Lambda:" + s.getName() + ":[ERROR]" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Async("asyncExec")
    public String getTextFromRekaPath(Long cognaId, String fileName, boolean fromCogna) {

        Document doc = null;

        if (!Helper.isNullOrEmpty(fileName)) {
            Path path;

            String rootUploadDir = Constant.UPLOAD_ROOT_DIR + "/attachment/";

            if (fromCogna) {
                path = Paths.get(rootUploadDir + "cogna-" + cognaId + "/" + fileName);
            } else {
                EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(fileName);
                if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                    path = Paths.get(rootUploadDir + "bucket-" + entryAttachment.getBucketId() + "/" + fileName);
                } else {
                    path = Paths.get(rootUploadDir + fileName);
                }
            }

            String mimeType;
            try {
                mimeType = Files.probeContentType(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

//            if (path.toString().endsWith(".pdf")) {
//                doc = loadDocument(path, new ApachePdfBoxDocumentParser());
//            }
//            if (path.toString().endsWith(".docx") || path.toString().endsWith(".doc") ||
//                    path.toString().endsWith(".pptx") || path.toString().endsWith(".ppt") ||
//                    path.toString().endsWith(".xlsx") || path.toString().endsWith(".xls")) {
//                doc = loadDocument(path, new ApachePoiDocumentParser());
//            }
//            if (path.toString().endsWith(".txt")) {
//                doc = loadDocument(path, new TextDocumentParser());
//            }

            if (mimeType.contains("image")) {
                String text = Helper.ocr(path.toString(), "eng");
                if (text == null || text.isEmpty()) {
                    return null;
                }
                doc = Document.from(text);
            } else {
                doc = loadDocument(path, new ApacheTikaDocumentParser());
            }
        } else {
            doc = Document.from("");
        }
        return doc.text();
    }

    @Async("asyncExec")
    public String getTextFromRekaURL(String url) {
        if (Helper.isNullOrEmpty(url)) {
            return "";
        }

        try {
            URLConnection connection = new URL(url).openConnection();
            String mimeType = connection.getContentType();
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            try (InputStream in = connection.getInputStream()) {
                if (mimeType.contains("image")) {
                    // OCR flow
                    Path tempFile = Files.createTempFile("reka-", "-img");
                    try {
                        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        String text = Helper.ocr(tempFile.toString(), "eng");
                        return (text == null || text.isEmpty()) ? "" : text;
                    } finally {
                        Files.deleteIfExists(tempFile);
                    }
                } else {
                    // Text/doc parsing flow
                    ContentHandler handler = new BodyContentHandler(10 * 1024 * 1024);
                    org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
                    Parser parser = new AutoDetectParser();
                    parser.parse(in, handler, metadata, new ParseContext());
                    return handler.toString().trim();
                }
            }
        } catch (IOException | TikaException | SAXException e) {
            throw new RuntimeException("Failed to extract text from URL: " + url, e);
        }
    }


    public boolean isImage(Long cognaId, String fileName, boolean fromCogna) {
        String mimeType = "";
        Path path = getPath(cognaId, fileName, fromCogna);

        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mimeType.contains("image");
    }

    public boolean isImageFromUrl(String url) {
        if (url == null || url.isBlank()) return false;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            String mimeType = conn.getContentType();
            conn.disconnect();
            return mimeType != null && mimeType.toLowerCase().startsWith("image");
        } catch (Exception e) {
            return false;
        }
    }

    public Path getPath(Long cognaId, String fileName, boolean fromCogna) {
        Path path;

        String rootUploadDir = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (fromCogna) {
            path = Paths.get(rootUploadDir + "cogna-" + cognaId + "/" + fileName);
        } else {
            EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(fileName);
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                path = Paths.get(rootUploadDir + "bucket-" + entryAttachment.getBucketId() + "/" + fileName);
            } else {
                path = Paths.get(rootUploadDir + fileName);
            }
        }

        return path;
    }

    public String getUrl(Long cognaId, String fileName, boolean fromCogna) {
        String url = IO_BASE_DOMAIN + "/api/cogna/" + cognaId + "/file/" + fileName;
        if (!fromCogna) {
            url = IO_BASE_DOMAIN + "/api/entry/file/inline/" + fileName;
        }
        return url;
    }

    private static final ObjectMapper GETJSONSCHEMA_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);


    public Map<String, String> getJsonFormatter(Long formId, boolean asSchema) {
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));
//        Map<String, Object> envelop = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();

//        envelop.put("$schema","https://json-schema.org/draft/2020-12/schema");
//        envelop.put("title", form.getTitle());
//        envelop.put("type", "object");

        form.getSections().forEach(section -> {
            if ("section".equals(section.getType())) {
                if (asSchema) {
                    processFormatting(form, section, properties);
                } else {
                    processFormattingSimple(form, section, properties);
                }
            } else if ("list".equals(section.getType())) {
                if (asSchema) {
                    Map<String, Object> schemaArray = new HashMap<>();
                    Map<String, Object> arrayProps = new HashMap<>();
                    schemaArray.put("type", "array");
                    processFormatting(form, section, arrayProps);
                    schemaArray.put("items", Map.of("type", "object", "properties", arrayProps));
                    properties.put(section.getCode(), schemaArray);
                } else {
                    Map<String, Object> arrayProps = new HashMap<>();
                    processFormatting(form, section, arrayProps);
                    properties.put(section.getCode(), List.of(arrayProps));
                }
            }
        });

        String jsonStr;
        try {
            jsonStr = GETJSONSCHEMA_MAPPER.writeValueAsString(properties);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return Map.of("schema", jsonStr);
    }

    private void processFormatting(Form form, Section section, Map<String, Object> sFormatter) {
        section.getItems().forEach(i -> {
            Item item = form.getItems().get(i.getCode());
            if (List.of("text").contains(item.getType())) {
                sFormatter.put(i.getCode(), Map.of(
                        "description", item.getLabel().trim(),
                        "type", "string"));
            } else if (List.of("file").contains(item.getType())) {
                if (List.of("imagemulti", "othermulti").contains(Optional.ofNullable(item.getSubType()).orElse(""))) {
                    sFormatter.put(i.getCode(), Map.of(
                            "description", item.getLabel().trim(),
                            "type", "array",
                            "items", Map.of("type", "string")));
                } else {
                    sFormatter.put(i.getCode(), Map.of(
                            "description", item.getLabel().trim(),
                            "type", "string"));
                }
            } else if (List.of("number", "scale", "scaleTo10", "scaleTo5").contains(item.getType())) {
                sFormatter.put(i.getCode(), Map.of(
                        "description", item.getLabel().trim(),
                        "type", "number"));
            } else if (List.of("date").contains(item.getType())) {
                sFormatter.put(i.getCode(), Map.of(
                        "description", item.getLabel().trim() + " as UNIX timestamp in miliseconds",
                        "type", "number"));
            } else if (List.of("checkbox").contains(item.getType())) {
                sFormatter.put(i.getCode(), Map.of(
                        "description", item.getLabel().trim(),
                        "type", "boolean"));
            } else if (List.of("select", "radio").contains(item.getType())) {
                if (List.of("multiple").contains(item.getSubType())) {
                    sFormatter.put(
                            i.getCode(),
                            Map.of("type", "array",
                                    "items", Map.of(
                                            "type", "object",
                                            "description", item.getLabel().trim(),
                                            "properties", Map.of(
                                                    "code", Map.of("type", "string"),
                                                    "name", Map.of("type", "string")
                                            )
                                    )
                            )
                    );
                } else {
                    sFormatter.put(
                            i.getCode(), Map.of(
                                    "type", "object",
                                    "description", item.getLabel().trim(),
                                    "properties", Map.of(
                                            "code", Map.of("type", "string"),
                                            "name", Map.of("type", "string")
                                    )
                            )
                    );
                }

            } else if (List.of("modelPicker").contains(item.getType())) {
                if (List.of("multiple").contains(item.getSubType())) {
                    sFormatter.put(
                            i.getCode(),
                            Map.of("type", "array",
                                    "items", Map.of(
                                            "type", "object",
                                            "description", item.getLabel().trim(),
                                            "properties", Map.of()
                                    )
                            )
                    );
                } else {
                    sFormatter.put(
                            i.getCode(), Map.of(
                                    "type", "object",
                                    "description", item.getLabel().trim(),
                                    "properties", Map.of()
                            )
                    );
                }

            } else if (List.of("map").contains(item.getType())) {
                sFormatter.put(
                        i.getCode(), Map.of(
                                "type", "object",
                                "description", item.getLabel().trim(),
                                "properties", Map.of(
                                        "longitude", Map.of("type", "number"),
                                        "latitude", Map.of("type", "number")
                                )
                        )
                );
            } else if (List.of("simpleOption").contains(item.getType())) {
                sFormatter.put(i.getCode(), Map.of(
                        "type", "string",
                        "description", item.getLabel().trim(),
                        "enum", Arrays.stream(item.getOptions().split(","))
                                .map(String::trim).toList()
                ));
            }
        });
    }

    private void processFormattingSimple(Form form, Section section, Map<String, Object> sFormatter) {
        section.getItems().forEach(i -> {
            Item item = form.getItems().get(i.getCode());
            if (List.of("text").contains(item.getType())) {
                sFormatter.put(i.getCode(), item.getLabel().trim() + " as string");
            } else if (List.of("file").contains(item.getType())) {
                if (List.of("imagemulti", "othermulti").contains(Optional.ofNullable(item.getSubType()).orElse(""))) {
                    sFormatter.put(i.getCode(),
                            item.getLabel().trim() + " as array of string (ie:['filename1.docx','filename2.docx'])");
                } else {
                    sFormatter.put(i.getCode(), item.getLabel().trim() + " as string (ie: 'filename.docx')");
                }
            } else if (List.of("number", "scale", "scaleTo10", "scaleTo5").contains(item.getType())) {
                sFormatter.put(i.getCode(), item.getLabel().trim() + " as number (ie: 10)");
            } else if (List.of("date").contains(item.getType())) {
                sFormatter.put(i.getCode(), item.getLabel().trim() + " as UNIX timestamp in miliseconds (ie: 1731044396197)");
            } else if (List.of("checkbox").contains(item.getType())) {
                sFormatter.put(i.getCode(), item.getLabel().trim() + " as boolean (ie: true)");
            } else if (List.of("select", "radio").contains(item.getType())) {
                if (List.of("multiple").contains(item.getSubType())) {
                    sFormatter.put(
                            i.getCode(),
                            item.getLabel().trim() + " as array of object (ie: [{code:'code1',name:'name1'},{code:'code2',name:'name2'}])"
                    );
                } else {
                    sFormatter.put(
                            i.getCode(),
                            item.getLabel().trim() + " as object (ie: {code:'code1', name:'name1'})"
                    );
                }
            } else if (List.of("modelPicker").contains(item.getType())) {
                if (List.of("multiple").contains(item.getSubType())) {
                    sFormatter.put(
                            i.getCode(),
                            item.getLabel().trim() + " as array of object "
                    );
                } else {
                    sFormatter.put(
                            i.getCode(),
                            item.getLabel().trim() + " as object"
                    );
                }
            } else if (List.of("map").contains(item.getType())) {
                sFormatter.put(
                        i.getCode(),
                        item.getLabel().trim() + " as object (ie: {longitude: -77.0364, latitude: 38.8951})"
                );
            } else if (List.of("simpleOption").contains(item.getType())) {
                sFormatter.put(i.getCode(),
                        item.getLabel().trim() + " as string from the following options: " + item.getOptions()
                );
            }
        });
    }

    Map<Long, ZooModel> zooModelMap = new HashMap<>();

    public List<ImagePredict> classifyImg(Long cognaId, String imageDir, String fileName) throws OrtException {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(() -> new ResourceNotFoundException("Cogna", "id", cognaId));

        if (!"imgcls".equals(cogna.getType())) throw new RuntimeException("Specified cogna is not image classifier");

        String fullPath = modelPath + "/" + cogna.getInferModelName();
        int limit = cogna.getData().at("/imgclsLimit").asInt(1);
        double minScore = cogna.getData().at("/imgclsMinScore").asDouble(8.00);

        var env = OrtEnvironment.getEnvironment();
        var session = ortSessionMap.getOrDefault(cogna.getInferModelName(),
                env.createSession(fullPath, new OrtSession.SessionOptions()));

        List<String> classes = Arrays.stream(cogna.getData().at("/imgclsCat").asText("").split("\n")).toList();

        ortSessionMap.putIfAbsent(cogna.getInferModelName(), session);

        // 1. Load model.
//        var session = env.createSession(modelPath, new OrtSession.SessionOptions());

        // Get input and output names
        var inputName = session.getInputNames().iterator().next();
        var outputName = session.getOutputNames().iterator().next();

        // 2. Create input tensor
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, Helper.processImage(imageDir + "/" + fileName, 1, 3, 224, 224));
//        OnnxTensor inputTensor = OnnxTensor.createTensor(env, Helper.processImage(imagePath,1,3,416,416));

        // 3. Run the model.
        var inputs = Map.of(inputName, inputTensor);
        var results = session.run(inputs);

        // 4. Get output tensor
        var outputTensor = results.get(outputName);

        if (outputTensor.isPresent()) {
            // 5. Get prediction results
            float[][] floatBuffer = (float[][]) outputTensor.get().getValue(); // for resnet, mobilenet
//            float[][][][][] floatBuffer = (float[][][][][]) outputTensor.get().getValue(); // for yolo

            List<ImagePredict> predictions2 = new ArrayList<>();

            // semua class akan di run. (1000x)
            for (int i = 0; i < floatBuffer[0].length; i++) {
                if (floatBuffer[0][i] > 5) {
                    predictions2.add(new ImagePredict(classes.get(i), i, floatBuffer[0][i], 0, 0, 0, 0));
                }
            }

            predictions2.sort(Comparator.comparingDouble(a -> -a.score()));
            predictions2 = predictions2.stream().filter(p -> p.score() > minScore).toList();
            if (predictions2.size() > 0) {
                predictions2 = predictions2.subList(0, limit);
            }

            return predictions2;
        } else {
            logger.error("Failed to predict!");
            return List.of();
        }
    }


    public DetectedObjects detectImgOld(Long cognaId, String imageDir, String fileName) throws IOException, ModelException, TranslateException {

        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(() -> new ResourceNotFoundException("Cogna", "id", cognaId));

        if (!"imgcls".equals(cogna.getType())) throw new RuntimeException("Specified cogna is not image classifier");

        Path imageFile = Paths.get(imageDir + "/" + fileName);
        ai.djl.modality.cv.Image img = ImageFactory.getInstance().fromFile(imageFile);

        Criteria<Path, DetectedObjects> criteria =
                Criteria.builder()
                        .setTypes(Path.class, DetectedObjects.class)
                        .optModelUrls("djl://ai.djl.onnxruntime/yolov8n/0.0.1/yolov8n")
//                        .optModelUrls("file://C:/var/yolov8n.pt")
//                        .optEngine("PyTorch")
                        .optArgument("width", 640)
                        .optArgument("height", 640)
                        .optArgument("resize", true)
                        .optArgument("toTensor", true)
                        .optArgument("applyRatio", true)
                        .optArgument("threshold", 0.6f)
                        // for performance optimization maxBox parameter can reduce number of
                        // considered boxes from 8400
                        .optArgument("maxBox", 1000)
//                        .optTranslatorFactory(new YoloV8TranslatorFactory())
                        .optProgress(new ProgressBar())
                        .build();

        try (ZooModel<Path, DetectedObjects> model = criteria.loadModel();
             Predictor<Path, DetectedObjects> predictor = model.newPredictor()) {
            Path outputPath = Paths.get("build/output");
            Files.createDirectories(outputPath);

            DetectedObjects detection = predictor.predict(imageFile);
            if (detection.getNumberOfObjects() > 0) {
                img.drawBoundingBoxes(detection);
                Path output = outputPath.resolve("C:/var/iris-files/yolov8_detected.png");
                try (OutputStream os = Files.newOutputStream(output)) {
                    img.save(os, "png");
                }
            }
            return detection;
        }
    }


    public List<ImagePredict> detectImg(Long cognaId, String imageDir, String fileName) throws OrtException, IOException {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(() -> new ResourceNotFoundException("Cogna", "id", cognaId));

        if (!"imgcls".equals(cogna.getType())) throw new RuntimeException("Specified cogna is not image classifier");

        String fullPath = modelPath + "/" + cogna.getInferModelName();
        int limit = cogna.getData().at("/imgclsLimit").asInt(1);
        double minScore = cogna.getData().at("/imgclsMinScore").asDouble(8.00);

        logger.info("model:" + cogna.getInferModelName());

        var env = OrtEnvironment.getEnvironment();
        var session = ortSessionMap.getOrDefault(cogna.getInferModelName(),
                env.createSession(fullPath, new OrtSession.SessionOptions()));

        List<String> classes = Arrays.stream(cogna.getData().at("/imgclsCat").asText("").split("\n")).toList();

        ortSessionMap.putIfAbsent(cogna.getInferModelName(), session);

        // 1. Load model.
//        var session = env.createSession(modelPath, new OrtSession.SessionOptions());

        // Get input and output names
        var inputName = session.getInputNames().iterator().next();
        var outputName = session.getOutputNames().iterator().next();

        // 2. Create input tensor
//        OnnxTensor inputTensor = OnnxTensor.createTensor(env, Helper.processImage(imagePath,1,3,224,224));

        Path imageFile = Paths.get(imageDir + "/" + fileName);
        ai.djl.modality.cv.Image img = ImageFactory.getInstance().fromFile(imageFile);
//
//        OnnxTensor.createTensor(env, ai.djl.modality.cv.Image)
        BufferedImage bi = Helper.processBufferedImageYolo(imageDir + "/" + fileName, 640, 640);

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, Helper.convertToFloatBuffer(bi, 1, 3, 640, 640));

        // 3. Run the model.
        var inputs = Map.of(inputName, inputTensor);
        var results = session.run(inputs);

        // 4. Get output tensor
        var outputTensor = results.get(outputName);

        if (outputTensor.isPresent()) {
            // 5. Get prediction results
//            float[][] floatBuffer = (float[][]) outputTensor.get().getValue(); // for resnet, mobilenet
            float[][][] floatBuffer = (float[][][]) outputTensor.get().getValue(); // for yolo

            List<ImagePredict> predictions2 = new ArrayList<>();

            Graphics2D graph = bi.createGraphics();

            for (int i = 0; i < floatBuffer[0].length; i++) {
                float x0 = floatBuffer[0][i][0];
                float y0 = floatBuffer[0][i][1];
                float x1 = floatBuffer[0][i][2];
                float y1 = floatBuffer[0][i][3];
                float confidence = floatBuffer[0][i][4];
                int label = (int) floatBuffer[0][i][5];
                if (confidence >= minScore) {

                    FontMetrics metrics = graph.getFontMetrics();

                    graph.setColor(Color.BLACK);

                    graph.drawRect((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0));

                    int lWidth = metrics.stringWidth(classes.get(label)) + 8;
                    int lHeight = metrics.getHeight();
                    graph.fillRect((int) x0, (int) y0 - lHeight, lWidth, lHeight);

                    graph.setColor(Color.WHITE);
                    graph.drawString(classes.get(label), (int) x0 + 4, (int) y0 - 4);

                    predictions2.add(new ImagePredict(classes.get(label), label, confidence, x0, y0, x1, y1));
                    logger.info(">>>RESULT[" + i + "]:" + x0 + "," + y0 + "," + x1 + "," + y1 + "," + classes.get(label) + "," + confidence);
                }

            }
            graph.dispose();

            try {
                ImageIO.write(bi, "png",
                        new File(imageDir + "/segmented-" + fileName));
                logger.info("writing image file");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return predictions2;
        } else {
            logger.info("Failed to predict!");
            return List.of();
        }
    }

    Map<String, OrtSession> ortSessionMap = new HashMap<>();

    public record ImagePredict(String desc, int index, double score, double x1, double y1, double x2, double y2) { }


    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down - closing all MCP clients...");

        // Close all MCP clients
        if (mcpClientsByCognaId != null) {
            for (List<McpClient> mcpClients : mcpClientsByCognaId.values()) {
                mcpClients.forEach(mcpClient -> {
                    try {
                        mcpClient.close();
                    } catch (Exception e) {
                        logger.error("Error closing MCP client on shutdown: " + e.getMessage());
                    }
                });
            }
            mcpClientsByCognaId.clear();
        }
        // Clear chat memories
        clearAllMemory();

        // Take care of embedding models
        if (e5Small != null) {
            try {
                if (e5Small instanceof AutoCloseable) {
                    ((AutoCloseable) e5Small).close();
                }
            } catch (Exception e) {
            } finally {
                e5Small = null;
            }
        }

        if (allMiniLm != null) {
            try {
                if (allMiniLm instanceof AutoCloseable) {
                    ((AutoCloseable) allMiniLm).close();
                }
            } catch (Exception e) {
            } finally {
                allMiniLm = null;
            }
        }

        if (ortSessionMap != null) {
            for (Map.Entry<String, OrtSession> entry : ortSessionMap.entrySet()) {
                String key = entry.getKey();
                OrtSession session = entry.getValue();
                if (session != null) {
                    try {
                        session.close(); // OrtSession implements AutoCloseable
                    } catch (Exception e) {
                    }
                }
            }
        }
        if (ortSessionMap != null) ortSessionMap.clear();
        if (assistantHolder != null) assistantHolder.clear();
        if (agentHolder != null) agentHolder.clear();
        if (textProcessorHolder != null) textProcessorHolder.clear();
        if (streamAssistantHolder != null) streamAssistantHolder.clear();
        if (storeHolder != null) storeHolder.clear();


    }


}
