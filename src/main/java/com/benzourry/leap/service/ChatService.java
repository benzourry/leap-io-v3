package com.benzourry.leap.service;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.translator.YoloTranslator;
import ai.djl.modality.cv.translator.YoloV8Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.translate.TranslatorContext;
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
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
//import dev.langchain4j.data.document.transformer.HtmlTextExtractor;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import dev.langchain4j.model.openai.*;
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
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.collection.DropCollectionParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.benzourry.leap.config.Constant.IO_BASE_DOMAIN;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;

@Service
public class ChatService {

    private final CognaRepository cognaRepository;

    private final CognaSourceRepository cognaSourceRepository;

    private final EntryAttachmentRepository entryAttachmentRepository;

    private final EntryService entryService;
    private final BucketService bucketService;

    private final LookupService lookupService;

    private final FormRepository formRepository;

    private final MailService mailService;


    private final LambdaService lambdaService;


    final String MILVUS = "milvus";
    final String CHROMADB = "chromadb";
    final String INMEMORY = "inmemory";
    final String MILVUS_HOST = "10.224.203.218";
    final int MILVUS_PORT = 19530;
    final String MILVUS_USER = "reka";
    final String MILVUS_PASSWORD = "[milvu5]";

    final String CHROMA_BASEURL = "http://10.224.203.218:8001";
    final int CHROMA_PORT = 8001;
    final String COLLECTION_PREFIX = "cogna_";
    final String DEFAULT_LOCALAI_BASEURL = "http://10.28.114.194:8080";

    @Value("${spring.profiles.active}")
    String APP_INSTANCE;

    @Value("${onnx.image-classification.model-path}")
    String modelPath;


    @PersistenceContext
    private EntityManager entityManager;


    public ChatService(CognaRepository cognaRepository,
                       CognaSourceRepository cognaSourceRepository,
                       EntryAttachmentRepository entryAttachmentRepository,
                       DatasetRepository datasetRepository,
                       EntryService entryService,
                       BucketService bucketService,
                       LookupService lookupService,
                       LambdaService lambdaService,
                       FormRepository formRepository,
                       MailService mailService) {
        this.cognaRepository = cognaRepository;
        this.cognaSourceRepository = cognaSourceRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.entryService = entryService;
        this.bucketService = bucketService;
        this.lookupService = lookupService;
        this.lambdaService = lambdaService;
        this.formRepository = formRepository;
        this.mailService = mailService;
    }

    //        Map<Long,ChatMemory> chatMemory = new HashMap<>();
    Map<Long, Map<String, ChatMemory>> chatMemoryMap = new HashMap<>();

//    public ChatMemory getChatHistory(Long cognaId, String email) {
//        return getChatMemory(cognaId, email);
//    }

//    static class LambdaRunner{
//
//        final String description;
//        public LambdaRunner(String description){
//            this.description = description;
//        }
//
//        @Tool(this.description)
//        int stringLength(String s) {
//            System.out.println("Called stringLength with s='" + s + "'");
//            return s.length();
//        }
//
//    }

//    static class Calculator {
//
//        @Tool("Reverse the word")
//        String stringReverse(String s) {
//            return "API";
////            System.out.println("Called stringLength with s='" + s + "'");
////            return s.length();
//        }
//
//        @Tool("Calculates the length of a string")
//        int stringLength(String s) {
//            System.out.println("Called stringLength with s='" + s + "'");
//            return s.length();
//        }
//
//        @Tool("Calculates the sum of two numbers")
//        int add(int a, int b) {
//            System.out.println("Called add with a=" + a + ", b=" + b);
//            return a + b;
//        }
//
//        @Tool("Calculates the square root of a number")
//        double sqrt(int x) {
//            System.out.println("Called sqrt with x=" + x);
//            return Math.sqrt(x);
//        }
//    }
//
////    public static class DynamicTool {
//        @Tool("{{toolDesc}}")
//        public int addTool(@V("toolDesc") String description, @P("JavaScript code to execute, result MUST be printed to console") String code, String input) {
//            System.out.println(code);
//            System.out.println(input);
//
//            return 0;
//        }
//
////    }

//    static class Tools {
//        @Tool("Extract data to json")
//        String outputJsonString(String s) {
////            System.out.println("Called stringLength with s='" + s + "'");
//            return s;
//        }
//    }

//    class BuiltInTools {
//
//        Long cognaId;
//        public BuiltInTools(Long cognaId){
//            this.cognaId = cognaId;
//        }
//
//        @Tool ("Generate image")
//        public String generateImage(@P("Description of the image") String text) {
//           return ChatService.this.generateImage(this.cognaId, text);
//        }
//
//    }

    interface TextProcessor {
        @UserMessage("Extract {{fields}} into json from {{text}}")
        String extract(@V("fields") String fields, @V("text") String text);

//        @UserMessage("Extract from {{text}} into the following json format {{format}}")
//        String extractJson(@V("format") String format, @V("text") String text);
//        @SystemMessage()
        @UserMessage({
                "Extract json from {{text}}.\n "
//                "As an example, for the schema {\"properties\": {\"foo\": {\"title\": \"Foo\", \"description\": \"a list of strings\", \"type\": \"array\", \"items\": {\"type\": \"string\"}}}, \"required\": [\"foo\"]}\n" +
//                        " the object {\"foo\": [\"bar\", \"baz\"]} is a well-formatted instance of the schema. " +
//                        " The object {\"properties\": {\"foo\": [\"bar\", \"baz\"]}} is not well-formatted.\n" +
//                "Here is the output schema: {{format}}"

//                "Return empty JSON if no data extracted."


//                "Extract data into json from {{text}}.\n " +
//                        "Here is the output schema: {{format}}\n" +
//                        "As an example, for the schema {\"properties\": {\"foo\": {\"title\": \"Foo\", \"description\": \"a list of strings\", \"type\": \"array\", \"items\": {\"type\": \"string\"}}}, \"required\": [\"foo\"]}\n" +
//                        " the object {\"foo\": [\"bar\", \"baz\"]} is a well-formatted instance of the schema. " +
//                        " The object {\"properties\": {\"foo\": [\"bar\", \"baz\"]}} is not well-formatted.\n"+
//
//                        "Return empty JSON if no data extracted."

        })
        String extractJson(@V("format") String format, @V("text") String text);

        @SystemMessage("You are a professional translator into {{language}}")
        @UserMessage("Translate the following text: {{text}}")
        String translate(@V("text") String text, @V("language") String language);

        @UserMessage("Analyze {{what}} of the following text and classify it into either {{classification}}: {{text}}")
        @SystemMessage("CRITICAL INSTRUCTION:\n" +
                "  - You must ONLY output TEXT from the following choices: {{classification}}\n" +
                "  - Do not include any explanatory text before or after the text\n" +
                "  - Do not use markdown code blocks\n" +
                "  - Do not include any additional formatting\n" +
                "STRICT RESPONSE FORMAT:\n" +
                "Return ONLY a TEXT from the choices ({{classification}}) - no additional text or explanations\n")
        String textClassification(@V("what")String what, @V("classification")String classification, @V("text")String text);

        @SystemMessage("Summarize every message from user in {{n}} bullet points. Provide only bullet points.")
        List<String> summarize(@UserMessage String text, @V("n") int n);
    }

    interface Assistant {

        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        String chat(@UserMessage String message, @V("systemMessage") String systemMessage);

        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        String chat(@UserMessage dev.langchain4j.data.message.UserMessage message, @V("systemMessage") String systemMessage);

        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        String chat(@UserMessage String message, @UserMessage ImageContent imageContent, @V("systemMessage") String systemMessage);
    }

    interface StreamingAssistant {
        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        TokenStream chat(@UserMessage String message, @V("systemMessage") String systemMessage);

        // not yet supported by Langchain4j:https://github.com/langchain4j/langchain4j/pull/1028
        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        TokenStream chat(@UserMessage dev.langchain4j.data.message.UserMessage message, @V("systemMessage") String systemMessage);
        // not yet supported by Langchain4j:https://github.com/langchain4j/langchain4j/pull/1028
        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        TokenStream chatWithImage(@UserMessage String message, @UserMessage Image image, @V("systemMessage") String systemMessage);

        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        TokenStream chat(@UserMessage String message, @UserMessage ImageContent imageContent, @V("systemMessage") String systemMessage);
    }

    Map<Long, Assistant> assistantHolder = new HashMap<>();
    Map<Long, TextProcessor> textProcessorHolder = new HashMap<>();
    Map<Long, StreamingAssistant> streamAssistantHolder = new HashMap<>();
    Map<Long, EmbeddingStore> storeHolder = new HashMap<>();

    EmbeddingModel allMiniLm = new AllMiniLmL6V2QuantizedEmbeddingModel();



    public ChatLanguageModel getChatLanguageModel(Cogna cogna, String responseFormat) {
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

                if ("json_schema".equals(responseFormat)){
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

                if ("json_schema".equals(responseFormat)){
                    oib.strictJsonSchema(true);
                }

                yield oib.build();
            }
            case "huggingface" -> HuggingFaceChatModel.builder()
                    .accessToken(cogna.getInferModelApiKey())
                    .modelId(cogna.getInferModelName())
                    .temperature(cogna.getTemperature())
                    .timeout(Duration.ofMinutes(10))
                    .waitForModel(true)
                    .build();
            case "localai" -> LocalAiChatModel.builder()
                    .modelName(cogna.getInferModelName())
                    .baseUrl(cogna.getData().at("/localAiBaseUrl").asText(DEFAULT_LOCALAI_BASEURL))
                    .temperature(cogna.getTemperature())
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

    public StreamingChatLanguageModel getStreamingChatLanguageModel(Cogna cogna) {
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
           case "localai" -> LocalAiStreamingChatModel.builder()
                    .modelName(cogna.getInferModelName())
                   .baseUrl(cogna.getData().at("/localAiBaseUrl").asText(DEFAULT_LOCALAI_BASEURL))
                    .temperature(cogna.getTemperature())
//                    .logResponses(true)
                    .logRequests(true)
//                    .maxTokens(cogna.getMaxToken())
                    .timeout(Duration.ofMinutes(10))
                    .build();

//            case "huggingface" -> HuggingFaceChatModel.builder()
//                    .accessToken(cogna.getInferModelApiKey())
//                    .modelId(cogna.getInferModelName())
//                    .temperature(cogna.getTemperature())
//                    .timeout(Duration.ofMinutes(10))
//                    .waitForModel(true)
//                    .build();
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
        if (chatMemoryMap.get(cogna.getId()) != null) {
            if (chatMemoryMap.get(cogna.getId()).get(email) != null) {
                thisChatMemory = chatMemoryMap.get(cogna.getId()).get(email);
            } else {
                chatMemoryMap.get(cogna.getId()).put(email, thisChatMemory);
            }
        } else {
            Map<String, ChatMemory> oneChatMemory = new HashMap<>();
            oneChatMemory.put(email, thisChatMemory);
            chatMemoryMap.put(cogna.getId(), oneChatMemory);
        }
        return thisChatMemory;
    }

    public EmbeddingModel getEmbeddingModel(Cogna cogna) {
        EmbeddingModel model;
        // no need to force classification to use allminilm
//        if ("txtcls".equals(cogna.getType())) {
//            model = allMiniLm;
//        } else {
            model = switch (cogna.getEmbedModelType()) {
                case "minilm" -> allMiniLm;
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
                        .baseUrl(cogna.getData().at("/localAiBaseUrl").asText(DEFAULT_LOCALAI_BASEURL))
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
//        }
        return model;
    }


    //    public List<EmbeddingMatch<TextSegment>> findSimilarity(Long cognaId, String search, int maxResult, Double minScore){
    public List<Map<String, Object>> findSimilarity(Long cognaId, String search, int maxResult, Double minScore) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        EmbeddingStore<TextSegment> es = getEmbeddingStore(cogna);

        EmbeddingModel em = getEmbeddingModel(cogna);
//        List<EmbeddingMatch<TextSegment>> matches;
        EmbeddingSearchResult<TextSegment> matches;
//        System.out.println("find similarity");
        if (minScore != null) {
            matches = es.search(EmbeddingSearchRequest.builder().queryEmbedding(em.embed(search).content())
                    .maxResults(maxResult)
                    .minScore(minScore).build());
//            matches = es.findRelevant(em.embed(search).content(), maxResult, minScore);
        } else {
            matches = es.search(EmbeddingSearchRequest.builder().queryEmbedding(em.embed(search).content())
                    .maxResults(maxResult).build());
//            matches = es.findRelevant(em.embed(search).content(), maxResult);
        }

//        System.out.println("findRelevant"+search+",matches:"+matches.matches().size());

        //        result.forEach(l->{
//            System.out.println(l.get("metadata"));
//        });
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

    public EmbeddingStore<TextSegment> getEmbeddingStore(Cogna cogna) {

        System.out.println("Collection: " + COLLECTION_PREFIX + APP_INSTANCE + "_" + cogna.getId() + ", DB:" + cogna.getVectorStoreType());

        if (storeHolder.get(cogna.getId()) != null) {
            return storeHolder.get(cogna.getId());
        } else {
//            boolean overrrideDb = cogna.getData().at("/overrideDb").asBoolean();
            var store = switch (cogna.getVectorStoreType()) {
//                String host = overrrideDb?cogna.getVectorStoreHost():MILVUS_HOST;
//                int port = overrrideDb?cogna.getVectorStorePort():MILVUS_PORT;
//                String username = overrrideDb.get
                case MILVUS -> MilvusEmbeddingStore.builder()
                        .host(MILVUS_HOST)
                        .port(MILVUS_PORT)
                        .username(MILVUS_USER)
                        .password(MILVUS_PASSWORD)
                        .collectionName(COLLECTION_PREFIX + APP_INSTANCE + "_" + cogna.getId())
                        .dimension(switch (cogna.getEmbedModelType()) {
                            case "openai" -> 1536;
                            case "huggingface" -> 384;
                            case "minilm" -> 384;
                            case "vertex-ai" -> 768;
                            default -> 1536;
                        })
                        .build();

                case CHROMADB -> ChromaEmbeddingStore.builder()
                        .baseUrl(CHROMA_BASEURL)
                        .collectionName(COLLECTION_PREFIX + APP_INSTANCE + "_" + cogna.getId())
                        .timeout(Duration.ofMinutes(10))
                        .build();
                case INMEMORY -> {
                    InMemoryEmbeddingStore<TextSegment> inMemStore;
                    File inMemoryStore = new File(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-"+ cogna.getId()+".store");
                    if (inMemoryStore.isFile()){
                        inMemStore = InMemoryEmbeddingStore.fromFile(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-"+ cogna.getId()+".store");
                    }else{
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


    /**
     * FOR LAMBDA
     **/
    public Map<String, Object> classify(Long cognaId, String text) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();

        if (cogna.getData().at("/txtclsLlm").asBoolean(false)){
            TextProcessor textProcessor;
            if (textProcessorHolder.get(cognaId) == null) {
                textProcessor = AiServices.create(TextProcessor.class, getChatLanguageModel(cogna, null));
                textProcessorHolder.put(cognaId, textProcessor);
            } else {
                textProcessor = textProcessorHolder.get(cognaId);
            }
            String what = cogna.getData().at("/txtclsWhat").asText("category");
            Long lookupId = cogna.getData().at("/txtclsLookupId").asLong();
            String classification = "";
            Map<String, LookupEntry> classificationMap = new HashMap<>();
            try {
                List<LookupEntry> entryList = (List<LookupEntry>)lookupService.findAllEntry(lookupId,null,null,true, PageRequest.of(0, Integer.MAX_VALUE)).getOrDefault("content", List.of());
                classification = entryList.stream()
                        .map(e->e.getName()).collect(Collectors.joining(", "));

                classificationMap = entryList.stream()
                        .collect(Collectors.toMap(LookupEntry::getName, entry -> entry));

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String category = textProcessor.textClassification(what,classification, text);

            Map<String, Object> returnVal = new HashMap<>();

            if (category!=null)
                returnVal.put("category", category);
            if (classificationMap.get(category) != null)
                returnVal.put("data",classificationMap.get(category));

            return returnVal;
        }else{
            EmbeddingModel embeddingModel = getEmbeddingModel(cogna);
            EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(cogna);

            Embedding queryEmbedding = embeddingModel.embed(text).content();
//            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 1);
            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(EmbeddingSearchRequest.builder()
                            .queryEmbedding(queryEmbedding)
                            .maxResults(1)
                    .build()).matches();
            EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

            Map<String, Object> returnVal = new HashMap<>();
            if (embeddingMatch.embedded().metadata().getString("category")!=null)
                returnVal.put("category",embeddingMatch.embedded().metadata().getString("category"));
            if (embeddingMatch.score()!=null)
                returnVal.put("score", embeddingMatch.score());

            return returnVal;
        }

    }

    /**
     * FOR LAMBDA
     **/
    public List<JsonNode> extract(Long cognaId, Map obj) {
        ObjectMapper om = new ObjectMapper();
        return extract(cognaId, om.convertValue(obj, CognaService.ExtractObj.class));
    }

//    public List<JsonNode> extractOld(Long cognaId, CognaService.ExtractObj extractObj) {
//        ObjectMapper mapper = new ObjectMapper();
//        TextProcessor textProcessor;
//        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
//        if (textProcessorHolder.get(cognaId) == null) {
//            textProcessor = AiServices
//                                .builder(TextProcessor.class)
//                                .chatLanguageModel(getChatLanguageModel(cogna, "json_object"))
//                                .build();
////                    AiServices.create(TextProcessor.class, getChatLanguageModel(cogna, "json_object"));
//            textProcessorHolder.put(cognaId, textProcessor);
//        } else {
//            textProcessor = textProcessorHolder.get(cognaId);
//        }
//
//        List<JsonNode> listData = new ArrayList<>();
//        if (extractObj.docList() != null) {
//            extractObj.docList().forEach(m -> {
//                try {
//                    String text = getTextFromRekaPath(cognaId, m, extractObj.fromCogna());
//                    String schema = cogna.getData()
//                            .at("/extractSchema")
//                            .asText();
//                    if (text!=null){
//                        listData.add(mapper.readTree(textProcessor.extractJson(schema, text)));
//                    }
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//        }
//        if (extractObj.text() != null) {
//            try {
//                listData.add(mapper.readTree(textProcessor.extractJson(cogna.getData()
//                        .at("/extractSchema")
//                        .asText(), extractObj.text())));
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        return listData;
//    }

    /**
     * FOR LAMBDA
     **/
    public String generateImage(Long cognaId, String text){
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        OpenAiImageModel model = new OpenAiImageModel.OpenAiImageModelBuilder()
                .modelName(Optional.ofNullable(cogna.getInferModelName()).orElse("dall-e-3"))
                .size(cogna.getData().at("/imgSize").asText("1024x1024"))
                .quality(cogna.getData().at("/imgQuality").asText("standard"))
                .style(cogna.getData().at("/imgStyle").asText("vivid"))
                .logRequests(true)
                .logResponses(true)
                .apiKey(cogna.getInferModelApiKey())
                .build();

//        model = OpenAiImageModel.withApiKey(cogna.getInferModelApiKey());
//        System.out.println(model.modelName());
        Response<Image> response = model.generate(text);

        String url = response.content().url().toString();

//        response.content().base64Data();

//        EntryAttachment ea;

        if (cogna.getData().at("/genBucket").asBoolean(false)){
            Long bucketId = cogna.getData().at("/genBucketId").asLong();
            if (bucketId!=null){
                EntryAttachment ea = bucketService.addUrlToBucket(bucketId,url, cogna.getApp().getId(), cogna.getEmail());
                url = IO_BASE_DOMAIN + "/api/entry/file/" + ea.getFileUrl();
            }
        }

        return url; // Donald Duck is here :)
    }

    public List<JsonNode> extract(Long cognaId, CognaService.ExtractObj extractObj) {
        if (!(extractObj!=null || extractObj.docList()!=null|| extractObj.text()!=null)){
            return List.of();
        }

        ObjectMapper mapper = new ObjectMapper();

        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(()->new ResourceNotFoundException("Cogna","id",cognaId));

        ChatLanguageModel model = getChatLanguageModel(cogna, "json_object");


        ToolParameters param;
        Map<String, Map<String, Object>> schemaMap;
//        Map<String, JsonSchemaElement> schemaMap;
        try {

            schemaMap = mapper.readValue(cogna.getData()
                    .at("/extractSchema")
                    .asText(),Map.class);
            param = ToolParameters.builder()
                    .properties(schemaMap)
                    .build();

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }



        ToolSpecification spec = ToolSpecification.builder()
                .name("extract")
                .description("Extract to json")
//                .parameters(JsonObjectSchema.builder().properties(schemaMap).build())
                .parameters(param)
                .build();

        List<JsonNode> listData = new ArrayList<>();
        if (extractObj.docList() != null) {
            extractObj.docList().parallelStream().forEach(m -> {
                try {
                    String text = getTextFromRekaPath(cognaId, m, extractObj.fromCogna());

                    if (text!=null && !text.isBlank()){
                        List<ChatMessage> messages = Collections.singletonList(
                                new dev.langchain4j.data.message.UserMessage("Extract json from text "+text)
                        );

                        listData.add(mapper.readTree(
                                model.generate(messages,spec).content().toolExecutionRequests().get(0).arguments()
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
                        new dev.langchain4j.data.message.UserMessage("Extract json from text "+extractObj.text())
                );

                listData.add(mapper.readTree(
                        model.generate(messages,spec).content().toolExecutionRequests().get(0).arguments())
                );

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return listData;
    }


    public Map<String,Object> imgcls(Long cognaId, CognaService.ExtractObj extractObj) {

        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(()->new ResourceNotFoundException("Cogna","id", cognaId));

        if (!(extractObj!=null || extractObj.docList()!=null)){
            return Map.of();
        }

        Map<String,Object> listData = new HashMap<>();
        if (extractObj.docList() != null) {
            extractObj.docList().parallelStream().forEach(m -> {
                try {
                    String filePath;
                    String fileDir;
                    if (extractObj.fromCogna()) {
                        filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/" + m;
                        fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId;
                    } else {
                        EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(m);
                        if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                            filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + entryAttachment.getBucketId() + "/" + m;
                            fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + entryAttachment.getBucketId();
                        } else {
                            filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/"+ m;
                            fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment";
                        }
                    }
//                    listData.put(m,
//                            detectImg(cognaId,fileDir, m)
//                    );
                    if (cogna.getInferModelName().contains("resnet")|| cogna.getInferModelName().contains("mobilenet")){
                        listData.put(m,
                                classifyImg(cognaId,fileDir, m)
                        );
                    }else if (cogna.getInferModelName().contains("yolo")){
                        listData.put(m,
                                detectImg(cognaId,fileDir, m)
                        );
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return listData;
    }

    public Map<String,List<ImagePredict>> imgclsOld(Long cognaId, CognaService.ExtractObj extractObj) {

        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(()->new ResourceNotFoundException("Cogna","id", cognaId));

        if (!(extractObj!=null || extractObj.docList()!=null)){
            return Map.of();
        }

        Map<String,List<ImagePredict>> listData = new HashMap<>();
        if (extractObj.docList() != null) {
            extractObj.docList().parallelStream().forEach(m -> {
                try {
                    String filePath;
                    String fileDir;
                    if (extractObj.fromCogna()) {
                        filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/" + m;
                        fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId;
                    } else {
                        EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(m);
                        if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                            filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + entryAttachment.getBucketId() + "/" + m;
                            fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + entryAttachment.getBucketId();
                        } else {
                            filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/"+ m;
                            fileDir = Constant.UPLOAD_ROOT_DIR + "/attachment";
                        }
                    }
                    listData.put(m,
                        detectImg(cognaId,fileDir,m)
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return listData;
    }


    /**
     * FOR LAMBDA
     **/
    public String translate(Long cognaId, String text, String language) {
//        ObjectMapper mapper = new ObjectMapper();
        TextProcessor textProcessor;
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        if (textProcessorHolder.get(cognaId) == null) {
//            AiServices.create(TextProcessor.class, getChatLanguageModel(cogna, null));

            textProcessor = AiServices
                    .builder(TextProcessor.class)
                    .chatLanguageModel(getChatLanguageModel(cogna, null))
                            .build();

            textProcessorHolder.put(cognaId, textProcessor);
        } else {
            textProcessor = textProcessorHolder.get(cognaId);
        }

        return textProcessor.translate(text, language);
    }

    /**
     * FOR LAMBDA
     **/
    public List<String> summarize(Long cognaId, String text, int pointCount) {
//        ObjectMapper mapper = new ObjectMapper();
        TextProcessor textProcessor;
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        if (textProcessorHolder.get(cognaId) == null) {
            textProcessor = AiServices.create(TextProcessor.class, getChatLanguageModel(cogna, null));
            textProcessorHolder.put(cognaId, textProcessor);
        } else {
            textProcessor = textProcessorHolder.get(cognaId);
        }
        return textProcessor.summarize(text, pointCount);
    }

    /**
     * FOR LAMBDA
     **/
    public String prompt(Long cognaId, Map obj, String email) {
        ObjectMapper om = new ObjectMapper();
        return prompt(email, cognaId, om.convertValue(obj, CognaService.PromptObj.class));
    }

    public String prompt(String email, Long cognaId, CognaService.PromptObj promptObj) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();

        // load chat memory
        ChatMemory thisChatMemory = getChatMemory(cogna, email);

        EmbeddingStore<TextSegment> embeddingStore;
        ChatLanguageModel chatModel;
        EmbeddingModel embeddingModel;


        Assistant assistant;

        String systemMessage = Optional.ofNullable(cogna.getSystemMessage()).orElse("Your name is Cogna");


        if (assistantHolder.get(cognaId) == null) {
            embeddingStore = getEmbeddingStore(cogna);

            String responseFormat = cogna.getData().at("/jsonOuput").asBoolean()?"json_schema":null;
//            if (cogna.getData().at("/jsonOuput").asBoolean())
            chatModel = getChatLanguageModel(cogna, responseFormat);
            embeddingModel = getEmbeddingModel(cogna);

            EmbeddingStoreContentRetriever.EmbeddingStoreContentRetrieverBuilder esrb = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(Optional.ofNullable(cogna.getEmbedMaxResult()).orElse(5));

            if (cogna.getEmbedMinScore() != null) {
                esrb.minScore(cogna.getEmbedMinScore());
            }


            AiServices<Assistant> assistantBuilder = AiServices
                    .builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .chatMemory(thisChatMemory);

            RetrievalAugmentor retrievalAugmentor = switch (Optional.ofNullable(cogna.getAugmentor()).orElse("")) {
                case "compressor" -> getQueryCompressorAugmentor(cogna,esrb.build(), chatModel);
                case "rerank" ->
                        getRerankAugmentor(cogna,esrb.build(), cogna.getData().at("/cohereApiKey").asText(), cogna.getData().at("/reRankMinScore").asDouble());
//                case "metadata" -> getMetadataAugmentor(esrb.build(), cogna.getData().at("/metadataKeys").asText(""));
                default -> getDefaultAugmentor(cogna,esrb.build());
            };

            assistantBuilder.retrievalAugmentor(retrievalAugmentor);


            Map<ToolSpecification, ToolExecutor> toolMap = new HashMap<>();
            // if imggenModel is defined, add it as tools to generate image
            if (cogna.getData().at("/imggenOn").asBoolean(false)){

                ToolSpecification toolSpecification = ToolSpecification.builder()
                        .name("generate_image")
                        .description("Generate image from the specified text")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("text", "Description of the image")
                                .build())
                        .build();

                ObjectMapper mapper = new ObjectMapper();

                ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
                    Map<String, Object> arguments;
                    try {
                        arguments = mapper.readValue(toolExecutionRequest.arguments(), HashMap.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    return generateImage(cogna.getData().at("/imggenCogna").asLong(), arguments.get("text").toString());
                };

                toolMap.put(toolSpecification, toolExecutor);
//
//                tools.add(new BuiltInTools(cogna.getData().at("/imggenCogna").asLong()));
////                assistantBuilder.tools(new BuiltInTools(cogna.getData().at("/imggenCogna").asLong()));
            }

            if (cogna.getTools().size()>0){
                cogna.getTools().forEach(ct->{

                    JsonObjectSchema.Builder joBuilder = JsonObjectSchema.builder();
                    ct.getParams().forEach(jsonNode->{
                        joBuilder.addStringProperty(jsonNode.get("key").asText(), jsonNode.get("description").asText());
                    });


                    ToolSpecification toolSpecification = ToolSpecification.builder()
                            .name("get_booking_details")
                            .description("Returns booking details")
                            .parameters(joBuilder.build())
                            .build();

                    ObjectMapper mapper = new ObjectMapper();

                    ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
                        Map<String, Object> arguments;
                        try {
                            arguments = mapper.readValue(toolExecutionRequest.arguments(), HashMap.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> executed = lambdaService.execLambda(ct.getLambdaId(),arguments,null,null,null, null);
                        return executed.get("print")+"";
                    };

                    toolMap.put(toolSpecification, toolExecutor);
                });
            }

            if (!toolMap.isEmpty()){
                assistantBuilder.tools(toolMap);
            }

            // if imggenModel is defined, add it as tools to generate image
//            if (cogna.getData().at("/imggenOn").asBoolean(false)){
//                assistantBuilder.tools(new BuiltInTools(cogna.getData().at("/imggenCogna").asLong()));
//            }

            assistant = assistantBuilder
                    .build();

            assistantHolder.put(cognaId, assistant);
        } else {
//            assistant = instanceHolder.get(cognaId).getAssistant();
            assistant = assistantHolder.get(cognaId);
        }

        List<Content> contentList = new ArrayList<>();
        List<String> textContentList = new ArrayList<>();

        String prompt = promptObj.prompt();

        if (cogna.getPostMessage() != null) {
            prompt += "\n\n" + cogna.getPostMessage();
//            textContentList.add(prompt);
        }

        if(cogna.getData().at("/jsonOutput").asBoolean()){
            systemMessage+= "\n\nCRITICAL INSTRUCTION:\n" +
                            "  - You must ONLY output a valid JSON object\n" +
                            "  - Do not include any explanatory text before or after the JSON\n" +
                            "  - Do not use markdown code blocks\n" +
                            "  - Do not include any additional formatting\n" +
                            "  - If you cannot provide accurate information, still return a valid JSON with empty arrays or zero values\n\n" +
                            "STRICT RESPONSE FORMAT:\n" +
                            "Return ONLY a JSON object with this exact structure - no additional text or explanations:\n" +
                                cogna.getData().at("/extractSchema").asText();
        }

        //START support multi-modal
        if (promptObj.fileList()!=null && promptObj.fileList().size()>0){
            boolean showScore = cogna.getData().at("/imgclsShowScore").asBoolean(false);
            promptObj.fileList().forEach(file->{
                String filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/" + file;
                if (isImage(cognaId, file, true)) {
                    // if enabled MultiModal support
                    if (Optional.ofNullable(cogna.getMmSupport()).orElse(false)) {
                        contentList.add(ImageContent.from(filePath));

                        //Alternative way to enable MM
                        if ("openai".equals(cogna.getInferModelType())){
                            ChatLanguageModel model = OpenAiChatModel.builder()
                                    .apiKey(cogna.getInferModelApiKey()) // Please use your own OpenAI API key
                                    .modelName(GPT_4_O_MINI)
                                    .logRequests(true)
                                    .logResponses(true)
                                    .maxTokens(50)
                                    .build();

                            Response<AiMessage> mmResponse = model.generate(
                                    dev.langchain4j.data.message.UserMessage.from(
                                            TextContent.from("Describe the image - no additional text or explanations"),
//                                        ImageContent.from("https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png")
                                            ImageContent.from(IO_BASE_DOMAIN+"/api/cogna/"+cognaId+"/file/"+file)
                                    )
                            );

                            String mmRepText = mmResponse.content().text();

                            textContentList.add("Included image: "+mmRepText);
                            System.out.println("MM identified Image: "+mmRepText);
                        }
                    }
                    // if enable image classification
                    if (cogna.getData().at("/imgclsOn").asBoolean(false)){
                        try {
                            List<ImagePredict> prediction = classifyImg(cogna.getData().at("/imgclsCogna").asLong(),
                                    Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId,
                                    file);

                            if (prediction.size() > 0) {
                                String text = prediction.stream().map(p -> p.desc() + (showScore?" (score: " + p.score() + ")":"")).collect(Collectors.joining("\n"));
                                contentList.add(TextContent.from("Image classified as : " + text));
                                textContentList.add("Image classified as : " + text);
                            }
                        } catch (Exception e) {
                            System.out.println("Error classifying image: "+e.getMessage());
//                            throw new RuntimeException(e);
                        }
                    }
                }
                // if enabled text extraction
                if (cogna.getData().at("/txtextractOn").asBoolean(false)){
                    String text = getTextFromRekaPath(cognaId, file, true);
                    if (text !=null && !text.isBlank()){
                        contentList.add(TextContent.from("Text in the attachment: " + text));
                        textContentList.add("Text in the attachment: " + text);
                    }
                }
            });
        }

        contentList.add(TextContent.from(prompt));
        textContentList.add(prompt);

        dev.langchain4j.data.message.UserMessage userMessage = dev.langchain4j.data.message.UserMessage.from(
                contentList
        );

//        return assistant.chat(userMessage, Optional.ofNullable(cogna.getSystemMessage()).orElse("Your name is Cogna"));
        return assistant.chat(String.join("\n\n",textContentList), systemMessage);


//        return assistant.chat(prompt, Optional.ofNullable(cogna.getSystemMessage()).orElse("Your name is Cogna"));
    }

    // ONLY SUPPORTED BY OPEN_AI WITH API KEY OR GEMINI PRO
    public TokenStream promptStream(String email, Long cognaId, CognaService.PromptObj promptObj) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();

        // load chat memory
        ChatMemory thisChatMemory = getChatMemory(cogna, email);
        EmbeddingStore<TextSegment> embeddingStore;
        StreamingChatLanguageModel chatModel;
        EmbeddingModel embeddingModel;


        StreamingAssistant assistant;

        String systemMessage = Optional.ofNullable(cogna.getSystemMessage()).orElse("Your name is Cogna");

        if (streamAssistantHolder.get(cognaId) == null) {
            System.out.println("assistant holder: ada");
            embeddingStore = getEmbeddingStore(cogna);

            String responseFormat = cogna.getData().at("/jsonOuput").asBoolean()?"json_schema":null;
            chatModel = getStreamingChatLanguageModel(cogna);
            embeddingModel = getEmbeddingModel(cogna);


            EmbeddingStoreContentRetriever.EmbeddingStoreContentRetrieverBuilder esrb = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(Optional.ofNullable(cogna.getEmbedMaxResult()).orElse(5));

            if (cogna.getEmbedMinScore() != null) {
                esrb.minScore(cogna.getEmbedMinScore());
            }

            AiServices<StreamingAssistant> assistantBuilder = AiServices
                    .builder(StreamingAssistant.class)
                    .streamingChatLanguageModel(chatModel)
                    .chatMemory(thisChatMemory);

            RetrievalAugmentor retrievalAugmentor = switch (Optional.ofNullable(cogna.getAugmentor()).orElse("")) {
                case "compressor" -> getQueryCompressorAugmentor(cogna,esrb.build(), getChatLanguageModel(cogna, null));
                case "rerank" -> getRerankAugmentor(cogna,esrb.build(), cogna.getData().at("/cohereApiKey").asText(), cogna.getData().at("/reRankMinScore").asDouble());
//                case "metadata" -> getMetadataAugmentor(esrb.build(), cogna.getData().at("/metadataKeys").asText(""));
                default -> getDefaultAugmentor(cogna,esrb.build());
            };

            assistantBuilder.retrievalAugmentor(retrievalAugmentor);

//            List<Object> tools = new ArrayList<>();

            Map<ToolSpecification, ToolExecutor> toolMap = new HashMap<>();

            // if imggenModel is defined, add it as tools to generate image
            if (cogna.getData().at("/imggenOn").asBoolean(false)){

                ToolSpecification toolSpecification = ToolSpecification.builder()
                        .name("generate_image")
                        .description("Generate image from the specified text")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("text", "Description of the image")
                                .build())
                        .build();

                ObjectMapper mapper = new ObjectMapper();

                ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
                    Map<String, Object> arguments;
                    try {
                        arguments = mapper.readValue(toolExecutionRequest.arguments(), HashMap.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    return generateImage(cogna.getData().at("/imggenCogna").asLong(), arguments.get("text").toString());
                };

                toolMap.put(toolSpecification, toolExecutor);
//
//                tools.add(new BuiltInTools(cogna.getData().at("/imggenCogna").asLong()));
////                assistantBuilder.tools(new BuiltInTools(cogna.getData().at("/imggenCogna").asLong()));
            }

            if (cogna.getTools().size()>0){

                UserPrincipal up = null;
                try {
                    up = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
                }catch(Exception e){}

                final UserPrincipal userPrincipal = up;

                cogna.getTools().forEach(ct->{

                    JsonObjectSchema.Builder joBuilder = JsonObjectSchema.builder();
                    ct.getParams().forEach(jsonNode->{
                        joBuilder.addStringProperty(jsonNode.get("key").asText(), jsonNode.get("description").asText());
                    });


                    ToolSpecification toolSpecification = ToolSpecification.builder()
                            .name(ct.getName())
                            .description(ct.getDescription())
                            .parameters(joBuilder.build())
                            .build();

                    ObjectMapper mapper = new ObjectMapper();

                    ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
                        Map<String, Object> arguments;
                        try {
                            arguments = mapper.readValue(toolExecutionRequest.arguments(), HashMap.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        Map<String, Object> executed = lambdaService.execLambda(ct.getLambdaId(),arguments,null,null,null, userPrincipal);
                        System.out.println("TOOL RESPONSE:::::::::::::"+executed.get("print")+"");
                        return executed.get("print")+"";
                    };

                    toolMap.put(toolSpecification, toolExecutor);
                });
            }

            if (!toolMap.isEmpty()){
                assistantBuilder.tools(toolMap);
            }


            assistant = assistantBuilder
                    .build();

            streamAssistantHolder.put(cognaId, assistant);
        } else {
            System.out.println("assistant holder: x ada");
            assistant = streamAssistantHolder.get(cognaId);
        }

        List<Content> contentList = new ArrayList<>();
        List<String> textContentList = new ArrayList<>();

        String prompt = promptObj.prompt();

        if (cogna.getPostMessage() != null) {
            prompt += "\n\n" + cogna.getPostMessage();
        }

        if(cogna.getData().at("/jsonOutput").asBoolean()){
            systemMessage+= "\n\nCRITICAL INSTRUCTION:\n" +
                    "  - You must ONLY output a valid JSON object\n" +
                    "  - Do not include any explanatory text before or after the JSON\n" +
                    "  - Do not use markdown code blocks\n" +
                    "  - Do not include any additional formatting\n" +
                    "  - If you cannot provide accurate information, still return a valid JSON with empty arrays or zero values\n\n" +
                    "STRICT RESPONSE FORMAT:\n" +
                    "Return ONLY a JSON object with this exact structure - no additional text or explanations:\n" +
                    cogna.getData().at("/extractSchema").asText();
        }

        //START support multi-modal
        if (promptObj.fileList()!=null && promptObj.fileList().size()>0){
            boolean showScore = cogna.getData().at("/imgclsShowScore").asBoolean(false);
            promptObj.fileList().forEach(file->{
                String filePath = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/" + file;
                if (isImage(cognaId, file, true)) {
                    // if enabled MultiModal support
                    if (Optional.ofNullable(cogna.getMmSupport()).orElse(false)) {
                        contentList.add(ImageContent.from(filePath));

                        //Alternative way to enable MM, AiService doesnt support multimodal
                        if ("openai".equals(cogna.getInferModelType())){
                            // if openai model, force using gpt4o-mini
                            ChatLanguageModel model = OpenAiChatModel.builder()
                                    .apiKey(cogna.getInferModelApiKey()) // Please use your own OpenAI API key
                                    .modelName(GPT_4_O_MINI)
                                    .logRequests(true)
                                    .logResponses(true)
                                    .maxTokens(50)
                                    .build();

                            Response<AiMessage> mmResponse = model.generate(
                                    dev.langchain4j.data.message.UserMessage.from(
                                            TextContent.from("Describe the image - no additional text or explanations"),
//                                        ImageContent.from("https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png")
                                            ImageContent.from(IO_BASE_DOMAIN+"/api/cogna/"+cognaId+"/file/"+file)
                                    )
                            );

                            String mmRepText = mmResponse.content().text();

                            textContentList.add("Included image: "+mmRepText);
                            System.out.println("MM identified Image: "+mmRepText);
                        }else{
                            // if not openai model, try to get multimodal response using the model
                            Response<AiMessage> mmResponse = getChatLanguageModel(cogna, null).generate(
                                    dev.langchain4j.data.message.UserMessage.from(
                                            TextContent.from("Describe the image - no additional text or explanations"),
                                            ImageContent.from(IO_BASE_DOMAIN+"/api/cogna/"+cognaId+"/file/"+file)
                                    )
                            );

                            String mmRepText = mmResponse.content().text();

                            textContentList.add("Included image: "+mmRepText);
                            System.out.println("MM identified Image: "+mmRepText);
                        }
                    }

                    // if enable image classification
                    if (cogna.getData().at("/imgclsOn").asBoolean(false)){
                        try {
                            List<ImagePredict> prediction = classifyImg(cogna.getData().at("/imgclsCogna").asLong(),
                                    Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId,
                                    file);

                            if (prediction.size() > 0) {
                                String text = prediction.stream().map(p -> p.desc() + (showScore?" (score: " + p.score() + ")":"")).collect(Collectors.joining("\n"));
                                contentList.add(TextContent.from("Image classified as : " + text));
                                textContentList.add("Image classified as : " + text);
                            }
                        } catch (Exception e) {
                            System.out.println("Error classifying image: "+e.getMessage());
//                            throw new RuntimeException(e);
                        }
                    }
                }
                // if enabled text extraction
                if (cogna.getData().at("/txtextractOn").asBoolean(false)){
                    String text = getTextFromRekaPath(cognaId, file, true);
                    if (text !=null && !text.isBlank()){
                        contentList.add(TextContent.from("Text in the attachment: " + text));
                        textContentList.add("Text in the attachment: " + text);
                    }
                }

            });
        }

        if (StringUtils.hasText(prompt)){
            contentList.add(TextContent.from(prompt));
            textContentList.add(prompt);
        }

        dev.langchain4j.data.message.UserMessage userMessage = dev.langchain4j.data.message.UserMessage.from(
                contentList
        );

//        return assistant.chat(userMessage, Optional.ofNullable(cogna.getSystemMessage()).orElse("Your name is Cogna"));

        return assistant.chat(String.join("\n\n",textContentList),systemMessage);

    }


    public Map<String, Object> clearMemoryByIdAndEmail(Long cognaId, String email) {
        System.out.println("Chat Memory====B4=====");
        chatMemoryMap.keySet().forEach(k -> {
            System.out.println("Key::" + k);
            chatMemoryMap.get(k).keySet().forEach(u -> System.out.println(u + ","));
            System.out.println("End key -----------");
        });

        if (chatMemoryMap.get(cognaId) != null) {
            chatMemoryMap.get(cognaId).remove(email);
        }
        System.out.println("Chat Memory=====AFter=====");
        chatMemoryMap.keySet().forEach(k -> {
            System.out.println("Key::" + k);
            chatMemoryMap.get(k).keySet().forEach(u -> System.out.println(u + ","));
            System.out.println("End key -----------");
        });


        if (assistantHolder.get(cognaId)!=null) assistantHolder.remove(cognaId);

        if (streamAssistantHolder.get(cognaId)!=null) streamAssistantHolder.remove(cognaId);

        return Map.of("success", true);
    }

    public Map<String, Object> clearMemoryById(Long cognaId) {
        chatMemoryMap.remove(cognaId);
        return Map.of("success", true);
    }

    public Map<String, Object> clearDb(Long cognaId) {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();
        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(cogna);

        if (MILVUS.equals(cogna.getVectorStoreType())) {
            embeddingStore.removeAll();
//            System.out.println("try clear milvusdb");
//            final MilvusServiceClient milvusClient = new MilvusServiceClient(
//                    ConnectParam.newBuilder()
//                            .withHost(MILVUS_HOST)
//                            .withPort(MILVUS_PORT)
//                            .withAuthorization(MILVUS_USER, MILVUS_PASSWORD)
//                            .build()
//            );
//
//            milvusClient.dropCollection(DropCollectionParam.newBuilder()
//                    .withCollectionName(COLLECTION_PREFIX + APP_INSTANCE + "_" + cognaId)
//                    .build());
//
//            milvusClient.close();
        }
        if (CHROMADB.equals(cogna.getVectorStoreType())) {
            embeddingStore.removeAll();
//            System.out.println("try clear chromadb");
//            HttpRequest request = HttpRequest.newBuilder()
//                    .DELETE()
//                    .uri(URI.create(CHROMA_BASEURL + "/api/v1/collections/" + COLLECTION_PREFIX + APP_INSTANCE + "_" + cognaId))
//                    .version(HttpClient.Version.HTTP_1_1)
//                    .build();
//
//            try {
//                HttpResponse<String> response = HttpClient.newHttpClient()
//                        .send(request, HttpResponse.BodyHandlers.ofString());
//                System.out.println(response.body());
//            } catch (IOException | InterruptedException e) {
//                System.out.println(e.getMessage());
//            }
        }
        if (INMEMORY.equals(cogna.getVectorStoreType())) {

            System.out.println("try clear imemory");
            File inMemoryStore = new File(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-"+ cogna.getId()+".store");
            if (inMemoryStore.isFile()){
                inMemoryStore.delete();
            }
            embeddingStore.removeAll();
            storeHolder.remove(cognaId);
        }
        reinitCogna(cognaId);

        cogna.getSources().forEach(s -> s.setLastIngest(null));
        cognaRepository.save(cogna);

        return Map.of("success", true);
    }

    public Map<String, Object> reinitCognaAndChatHistory(Long cognaId) {
        assistantHolder.remove(cognaId);
        streamAssistantHolder.remove(cognaId);
        storeHolder.remove(cognaId);
        chatMemoryMap.remove(cognaId);
        textProcessorHolder.remove(cognaId);
        return Map.of("success", true);
    }

    public Map<String, Object> reinitCogna(Long cognaId) {
        assistantHolder.remove(cognaId);
        streamAssistantHolder.remove(cognaId);
        storeHolder.remove(cognaId);
        textProcessorHolder.remove(cognaId);
//        chatMemoryMap.remove(cognaId);
        return Map.of("success", true);
    }

    public Map<String, Object> clearAllMemory() {
        chatMemoryMap.clear();
        return Map.of("success", true);
    }

    // transactional needed for Stream dataset
    @Transactional
    public Map<Long, Map> ingest(Long cognaId) {

        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow();

        Map<Long, Map> data = new HashMap<>();
        cogna.getSources()
                .stream()
                .forEach(s -> data.put(s.getId(), ingestSource(s)));

        return data;
    }

    public void persistInMemoryVectorStore(Cogna cogna){
        if (INMEMORY.equals(cogna.getVectorStoreType())){
            File inMemoryStoreDir = new File(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store");
            inMemoryStoreDir.mkdirs();
            InMemoryEmbeddingStore<TextSegment> store = (InMemoryEmbeddingStore<TextSegment>) getEmbeddingStore(cogna);
            store.serializeToFile(Constant.UPLOAD_ROOT_DIR + "/cogna-inmemory-store/cogna-inmemory-"+ cogna.getId()+".store");
        }
    }
    @Transactional
    public Map<String, Object> ingestSource(CognaSource cognaSrc) {

        Cogna cogna = cognaSrc.getCogna();

        long ingestStart = System.currentTimeMillis();

        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore(cogna);

        EmbeddingModel embeddingModel = getEmbeddingModel(cogna);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentTransformer(document->{
                    if (document.metadata().getString("file_name")!=null)
                        document.metadata().put("web_url",IO_BASE_DOMAIN+"/api/entry/file/"+document.metadata().getString("file_name"));

                    return document;
                })
                .documentSplitter(DocumentSplitters.recursive(
                        Optional.ofNullable(cogna.getChunkLength()).orElse(100),
                        Optional.ofNullable(cogna.getChunkOverlap()).orElse(10),
                        new OpenAiTokenizer("gpt-3.5-turbo")))
                .textSegmentTransformer(textSegment -> {
                        if (textSegment.metadata().getString("file_name")!=null)
                            textSegment.metadata().put("web_url",IO_BASE_DOMAIN+"/api/entry/file/"+textSegment.metadata().getString("file_name"));

                        return TextSegment.from(
                            textSegment.text(),
                            textSegment.metadata());
                    }
                )
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        AtomicInteger docCount = new AtomicInteger();
//        Map<Long, String> errorMap = new HashMap<>();
//        Map<Long, Boolean> errorStatus = new HashMap<>();
//        cogna.getSources().forEach(s -> {
        if ("bucket".equals(cognaSrc.getType())) {
            Page<EntryAttachment> attachmentPage = entryAttachmentRepository.findByBucketId(cognaSrc.getSrcId(), "%", PageRequest.of(0, 999));
//
            try {
                File dir = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cogna.getId());
                dir.mkdirs();

                Path path = Paths.get(Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cogna.getId() + "/bucket-" + cognaSrc.getSrcId() + ".txt");
                if (cognaSrc.getLastIngest() == null) { // mn xpernah knak ingest, make sure null, so, mn clear db + ingest date == null
                    Files.deleteIfExists(path);
                }

                FileWriter fw = new FileWriter(path.toFile(), true);

                String errorMsg = "";
                attachmentPage.forEach(at -> {

                    // ingest only when source not yet ingested OR attachment is uploaded after last ingest
                    // problem cara tok, plain text nya x menggambarkan embeddings sbb partial jk\
                    // ataupun, just append without reset
                    if (cognaSrc.getLastIngest() == null || at.getTimestamp().after(cognaSrc.getLastIngest())) {
                        try {

                            Document doc = null;
                            File f = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/bucket-" + cognaSrc.getSrcId() + "/" + at.getFileUrl());
                            if ("application/pdf".equals(at.getFileType())) {
                                System.out.println("is pdf");
                                doc = loadDocument(f.toPath(), new ApachePdfBoxDocumentParser());
//                                docList.add(doc);
                            }
                            if (at.getFileUrl().contains(".docx") || at.getFileUrl().contains(".pptx") || at.getFileUrl().contains(".xlsx")) {
                                doc = loadDocument(f.toPath(), new ApachePoiDocumentParser());
//                                docList.add(doc);
                            }
                            if ("text/plain".equals(at.getFileType())) {
                                doc = loadDocument(f.toPath(), new TextDocumentParser());
//                                docList.add(doc);
                            }
                            if (at.getFileType().contains("image")) {
                                String text = Helper.ocr(path.toString(), "eng");
                                doc = Document.from(text);
                            }

                            System.out.println("doc:"+doc);
                            if (doc != null) {
//                                    docList.add(doc);
                                ingestor.ingest(doc);
                                docCount.getAndIncrement();
                                try {
                                    fw.write(doc.text() + "\n\n");
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                            }

                        } catch (Exception e) {
                            System.out.println("Error ingest ("+cognaSrc.getName()+"):"+e.getMessage());
                        }
                    }
                });

                fw.close();
                cognaSrc.setLastIngest(new Date());
                cognaSourceRepository.save(cognaSrc);

                persistInMemoryVectorStore(cogna);

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        if ("dataset".equals(cognaSrc.getType())) {
            try {
                File dir = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cogna.getId());
                dir.mkdirs();

                Path path = Paths.get(Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cogna.getId() + "/dataset-" + cognaSrc.getSrcId() + ".txt");
                Files.deleteIfExists(path);
                ingestDataset(embeddingStore, embeddingModel, path, cognaSrc, cognaSrc.getSrcId(), "%", null, null, null, null, null);

                docCount.getAndIncrement();
                /* Ingest per entry terus dlm ingestDataset()
                Document doc = loadDocument(path, new TextDocumentParser());
                ingestor.ingest(doc);
                docCount.getAndIncrement();
                 */

                cognaSrc.setLastIngest(new Date());
                cognaSourceRepository.save(cognaSrc);

                persistInMemoryVectorStore(cogna);

            } catch (IOException e) {
                System.out.println("Error line 950:"+ e.getMessage());
            }
        }
        if ("url".equals(cognaSrc.getType())) {

            try {
                File dir = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cogna.getId());
                dir.mkdirs();

                Path path = Paths.get(Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cogna.getId() + "/web-" + cognaSrc.getId() + ".txt");
//                    Path path = Paths.get("C:/var/iris-files/cogna-"+cognaId + "/web-"+s.getId()+".txt");
//                org.jsoup.nodes.Document webdoc = Jsoup.connect(cognaSrc.getSrcUrl()).get();
//                Files.writeString(path, webdoc.body().text());

                Document htmlDoc = UrlDocumentLoader.load(cognaSrc.getSrcUrl(), new TextDocumentParser());
//                HtmlTextExtractor transformer = new HtmlTextExtractor(null, null, true);
                HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer();
                Document doc = transformer.transform(htmlDoc);

                Files.writeString(path, doc.text());

//                Document doc = loadDocument(path, new TextDocumentParser());
//                    docList.add(doc);
                ingestor.ingest(doc);
                docCount.getAndIncrement();
                cognaSrc.setLastIngest(new Date());
                cognaSourceRepository.save(cognaSrc);

                persistInMemoryVectorStore(cogna);
            } catch (IOException e) {
                System.out.println("Error line 979:"+e.getMessage());
            }

        }
//        });

        long ingestEnd = System.currentTimeMillis();

//        reinitCogna(cogna.getId());


        return Map.of("success", true, "docCount", docCount.get(), "timeMilis", (ingestEnd - ingestStart));

    }


            // .promptTemplate(...) // Formatting can also be changed
//            .metadataKeysToInclude(asList("file_name", "index"));
//            .build();

    public RetrievalAugmentor getDefaultAugmentor(Cogna cogna,ContentRetriever contentRetriever) {
        DefaultContentInjector.DefaultContentInjectorBuilder contentInjector = DefaultContentInjector.builder();
        // DEFAULT AUGMENTOR IALAH EMPTY AUGMENTOR WITH METADATA
        // Each retrieved segment should include "file_name" and "index" metadata values in the prompt
        if (cogna.getData().at("/withMetadata").asBoolean(false)){
            System.out.println("with metadata");
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

    public RetrievalAugmentor getQueryCompressorAugmentor(Cogna cogna,ContentRetriever contentRetriever, ChatLanguageModel chatModel) {
        DefaultContentInjector.DefaultContentInjectorBuilder contentInjector = DefaultContentInjector.builder();
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel);
        // The RetrievalAugmentor serves as the entry point into the RAG flow in LangChain4j.
        // It can be configured to customize the RAG behavior according to your requirements.
        // In subsequent examples, we will explore more customizations.

        if (cogna.getData().at("/withMetadata").asBoolean(false)){
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

    public RetrievalAugmentor getRerankAugmentor(Cogna cogna,ContentRetriever contentRetriever, String cohereApiKey, Double reRankMinScore) {

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

        if (cogna.getData().at("/withMetadata").asBoolean(false)){
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


//    private static Path toPath(String fileName) {
//        try {
//            URL fileUrl = ChatWithDocumentsExamples.class.getResource(fileName);
//            return Paths.get(fileUrl.toURI());
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//    }


    @Transactional
    public Map<String, Object> ingestDataset(EmbeddingStore<TextSegment> store,
                                             EmbeddingModel embeddingModel,
                                             Path txtPath, CognaSource cognaSrc, Long datasetId, String searchText,
                                             String email, Map filters, String cond, List<Long> ids, HttpServletRequest req) throws IOException {

        Map<String, Object> data = new HashMap<>();
        System.out.println("Dalam ingestDataset()");
//        Dataset d = datasetRepository.getReferenceById(datasetId);
//        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow();

//        Page<Entry> list = findListByDataset(datasetId, searchText, email, filters, PageRequest.of(0, Integer.MAX_VALUE), req);

        AtomicInteger index = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();

        FileWriter fw = new FileWriter(txtPath.toFile(), true);

//        List<TextSegment> listSegment = new ArrayList<>();


        try (Stream<Entry> entryStream = entryService.findListByDatasetStream(datasetId, searchText, email, filters, null, null, ids, req)) {
            entryStream.forEach(entry -> {

                String sentence = toTextSentence(cognaSrc.getSentenceTpl(), entry);
                String category = "";
                if (cognaSrc.getCategoryTpl() != null) {
                    category = toTextSentence(cognaSrc.getCategoryTpl(), entry);
                }


                TextSegment segment1 = TextSegment.from(Helper.html2text(sentence), Metadata.from(
                    Map.of(
                    "category", category,
                    "dataset", String.valueOf(datasetId)
                    )
                ));

//                listSegment.add(segment1);

                Embedding embedding1 = embeddingModel.embed(segment1).content();

                store.add(embedding1, segment1);

                try {
                    fw.write(Helper.html2text(sentence) + "\n");
//                    System.out.println(sentence);
                    index.getAndIncrement();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                total.getAndIncrement();
                this.entityManager.detach(entry);
            });
        }

        fw.close();

        System.out.println("TOTAL:::" + total.get());

        data.put("totalCount", total.get());
        data.put("totalSegment", index.get());
        data.put("success", total.get() == index.get());
        data.put("partial", total.get() > index.get());
        return data;

    }

    public String toTextSentence(String sentenceTpl, Entry entry) {
        if (sentenceTpl == null) {
            return "";
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("_", entry);
        Map<String, Object> result = mapper.convertValue(entry.getData(), Map.class);
        Map<String, Object> prev = mapper.convertValue(entry.getPrev(), Map.class);

        String url = "https://" + entry.getForm().getApp().getAppPath() + "." + Constant.UI_BASE_DOMAIN + "/#";
        dataMap.put("uiUri", url);
        dataMap.put("viewUri", url + "/form/" + entry.getForm().getId() + "/view?entryId=" + entry.getId());
        dataMap.put("editUri", url + "/form/" + entry.getForm().getId() + "/edit?entryId=" + entry.getId());

//        List<String> recipients = new ArrayList<>();// Arrays.asList(entry.getEmail());
//        List<String> recipientsCc = new ArrayList<>();


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

//        String afterRewrite = MailService.rewriteTemplate(sentenceTpl);
//        System.out.println(afterRewrite);
        return MailService.compileTpl(Optional.ofNullable(sentenceTpl).orElse(""), dataMap);

    }

    @Scheduled(cron = "0 0/10 * * * ?") //0 */1 * * * *
    public Map<String, Object> runSchedule() {

        Calendar now = Calendar.getInstance();

        String clock = String.format("%02d%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        int day = now.get(Calendar.DAY_OF_WEEK); // sun=1, mon=2, tues=3,wed=4,thur=5,fri=6,sat=7
        int date = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH); // 0-based month, ie: Jan=0, Feb=1, March=2

        cognaSourceRepository.findScheduledByClock(clock).forEach(s -> {
            if ("daily".equals(s.getFreq()) ||
                    ("weekly".equals(s.getFreq()) && s.getDayOfWeek() == day) ||
                    ("monthly".equals(s.getFreq()) && s.getDayOfMonth() == date) ||
                    ("yearly".equals(s.getFreq()) && s.getMonthOfYear() == month && s.getDayOfMonth() == date)
            ) {
                try {
                    long start = System.currentTimeMillis();
                    ingestSource(s);
                    long end = System.currentTimeMillis();
                    System.out.println("Duration ingest ("+s.getName()+"):" + (end - start));
                } catch (Exception e) {
                    System.out.println("ERROR executing Lambda:" + s.getName());
                }
            }
        });
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


            if (path.toString().endsWith(".pdf")) {
                doc = loadDocument(path, new ApachePdfBoxDocumentParser());
            }
            if (path.toString().endsWith(".docx") || path.toString().endsWith(".doc") ||
                    path.toString().endsWith(".pptx") || path.toString().endsWith(".ppt") ||
                    path.toString().endsWith(".xlsx") || path.toString().endsWith(".xls")) {
                doc = loadDocument(path, new ApachePoiDocumentParser());
            }
            if (path.toString().endsWith(".txt")) {
                doc = loadDocument(path, new TextDocumentParser());
            }

            if (mimeType.contains("image")) {
                String text = Helper.ocr(path.toString(), "eng");
                if (text==null || text.isEmpty()){
                    return null;
                }
                doc = Document.from(text);
            }
        }else{
            doc = Document.from("");
        }
        return doc.text();
    }


    public boolean isImage(Long cognaId, String fileName, boolean fromCogna){
        String mimeType = "";
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

        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mimeType.contains("image");
    }


//    public Map<String, String> getJsonFormatterOld(Long formId){
//        Form form = formRepository.findById(formId).orElseThrow(()->new ResourceNotFoundException("Form","id",formId));
//        Map<String, Object> formatter = new HashMap<>();
//
//        form.getSections().forEach(section->{
//            if ("section".equals(section.getType())){
//                processFormatting(form, section, formatter);
//            }
//            if ("list".equals(section.getType())){
//                Map<String, Object> sFormatter = new HashMap<>();
//                processFormatting(form, section, sFormatter);
//                formatter.put(section.getCode(), List.of(sFormatter));
//            }
//        });
//
//        String fmtStr = convertMapToSchema(formatter);
//        String jsonStr = convertMapToJson(formatter);
//
//        return Map.of("fmt",fmtStr,"json",jsonStr);
//    }
    public Map<String, String> getJsonFormatter(Long formId, boolean asSchema){
        Form form = formRepository.findById(formId).orElseThrow(()->new ResourceNotFoundException("Form","id",formId));
//        Map<String, Object> envelop = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();

//        envelop.put("$schema","https://json-schema.org/draft/2020-12/schema");
//        envelop.put("title", form.getTitle());
//        envelop.put("type", "object");

        form.getSections().forEach(section->{
            if ("section".equals(section.getType())){
                if (asSchema){
                    processFormatting(form, section, properties);
                }else{
                    processFormattingSimple(form, section, properties);
                }
            }
            if ("list".equals(section.getType())){
                if (asSchema) {
                    Map<String, Object> schemaArray = new HashMap<>();
                    Map<String, Object> arrayProps = new HashMap<>();
                    schemaArray.put("type", "array");
                    processFormatting(form, section, arrayProps);
                    schemaArray.put("items", Map.of("type", "object", "properties", arrayProps));
                    properties.put(section.getCode(), schemaArray);
                }else{
//                    Map<String, Object> schemaArray = new HashMap<>();
                    Map<String, Object> arrayProps = new HashMap<>();
//                    schemaArray.put("type", "array");
                    processFormatting(form, section, arrayProps);
//                    schemaArray.put("items", Map.of("type", "object", "properties", arrayProps));
                    properties.put(section.getCode(), List.of(arrayProps));
                }
            }
        });

//        envelop.put("properties", properties);
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        String jsonStr;
        try {
            jsonStr = mapper.writeValueAsString(properties);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


//        String fmtStr = convertMapToSchema(properties);
//        String jsonStr = convertMapToJson(properties);

        return Map.of("schema",jsonStr);
    }

    private void processFormatting(Form form, Section section, Map<String, Object> sFormatter) {
        section.getItems().forEach(i->{
            Item item = form.getItems().get(i.getCode());
            if (List.of("text").contains(item.getType())){
                sFormatter.put(i.getCode(),Map.of(
                        "description",item.getLabel().trim(),
                        "type","string"));
            }else if (List.of("file").contains(item.getType())){
                if (List.of("imagemulti", "othermulti").contains(Optional.ofNullable(item.getSubType()).orElse(""))){
                    sFormatter.put(i.getCode(),Map.of(
                            "description",item.getLabel().trim(),
                            "type","array",
                            "items", Map.of("type","string")));
                }else{
                    sFormatter.put(i.getCode(),Map.of(
                            "description",item.getLabel().trim(),
                            "type","string"));
                }
            }else if (List.of("number", "scale", "scaleTo10", "scaleTo5").contains(item.getType())){
                sFormatter.put(i.getCode(),Map.of(
                        "description",item.getLabel().trim(),
                        "type","number"));
            }else if (List.of("date").contains(item.getType())){
                sFormatter.put(i.getCode(),Map.of(
                        "description",item.getLabel().trim() + " as UNIX timestamp in miliseconds",
                        "type","number"));
            }else if (List.of("checkbox").contains(item.getType())){
                sFormatter.put(i.getCode(),Map.of(
                        "description",item.getLabel().trim(),
                        "type","boolean"));
            }else if (List.of("select", "radio").contains(item.getType())){
                if (List.of("multiple").contains(item.getSubType())){
                    sFormatter.put(
                            i.getCode(),
                            Map.of("type","array",
                                    "items", Map.of(
                                            "type","object",
                                            "description", item.getLabel().trim(),
                                            "properties", Map.of(
                                                    "code", Map.of("type","string"),
                                                    "name",Map.of("type","string")
                                            )
                                    )
                            )
                    );
                }else{
                    sFormatter.put(
                            i.getCode(), Map.of(
                                    "type","object",
                                    "description", item.getLabel().trim(),
                                    "properties", Map.of(
                                            "code", Map.of("type","string"),
                                            "name", Map.of("type","string")
                                    )
                            )
                    );
                }

            }else if (List.of("modelPicker").contains(item.getType())){
                if (List.of("multiple").contains(item.getSubType())){
                    sFormatter.put(
                            i.getCode(),
                            Map.of("type","array",
                                    "items",Map.of(
                                            "type","object",
                                            "description", item.getLabel().trim(),
                                            "properties", Map.of()
                                    )
                            )
                    );
                }else{
                    sFormatter.put(
                            i.getCode(), Map.of(
                                    "type","object",
                                    "description", item.getLabel().trim(),
                                    "properties", Map.of()
                            )
                    );
                }

            }else if (List.of("map").contains(item.getType())){
                sFormatter.put(
                        i.getCode(), Map.of(
                                "type","object",
                                "description", item.getLabel().trim(),
                                "properties", Map.of(
                                        "longitude", Map.of("type","number"),
                                        "latitude",Map.of("type","number")
                                )
                        )
                );
            }else if(List.of("simpleOption").contains(item.getType())){
                sFormatter.put(i.getCode(), Map.of(
                        "type","string",
                        "description", item.getLabel().trim(),
                        "enum",Arrays.stream(item.getOptions().split(","))
                                .map(String::trim).toList()
                ));
            }
        });
    }

    private void processFormattingSimple(Form form, Section section, Map<String, Object> sFormatter) {
        section.getItems().forEach(i->{
            Item item = form.getItems().get(i.getCode());
            if (List.of("text").contains(item.getType())){
                sFormatter.put(i.getCode(),item.getLabel().trim() + " as string");
            }else if (List.of("file").contains(item.getType())){
                if (List.of("imagemulti", "othermulti").contains(Optional.ofNullable(item.getSubType()).orElse(""))){
                    sFormatter.put(i.getCode(),
                            item.getLabel().trim() + " as array of string (ie:['filename1.docx','filename2.docx'])");
                }else{
                    sFormatter.put(i.getCode(),item.getLabel().trim()+ " as string (ie: 'filename.docx')");
                }
            }else if (List.of("number", "scale", "scaleTo10", "scaleTo5").contains(item.getType())){
                sFormatter.put(i.getCode(),item.getLabel().trim()+" as number (ie: 10)");
            }else if (List.of("date").contains(item.getType())){
                sFormatter.put(i.getCode(),item.getLabel().trim() + " as UNIX timestamp in miliseconds (ie: 1731044396197)");
            }else if (List.of("checkbox").contains(item.getType())){
                sFormatter.put(i.getCode(),item.getLabel().trim()+" as boolean (ie: true)");
            }else if (List.of("select", "radio").contains(item.getType())){
                if (List.of("multiple").contains(item.getSubType())){
                    sFormatter.put(
                            i.getCode(),
                            item.getLabel().trim() + " as array of object (ie: [{code:'code1',name:'name1'},{code:'code2',name:'name2'}])"
                    );
                }else{
                    sFormatter.put(
                            i.getCode(),
                            item.getLabel().trim() + " as object (ie: {code:'code1', name:'name1'})"
                    );
                }

            }else if (List.of("modelPicker").contains(item.getType())){
                if (List.of("multiple").contains(item.getSubType())){
                    sFormatter.put(
                            i.getCode(),
                            item.getLabel().trim() + " as array of object "
                    );
                }else{
                    sFormatter.put(
                            i.getCode(),
                            item.getLabel().trim() + " as object"
                    );
                }

            }else if (List.of("map").contains(item.getType())){
                sFormatter.put(
                        i.getCode(),
                        item.getLabel().trim() + " as object (ie: {longitude: -77.0364, latitude: 38.8951})"
                );
            }else if(List.of("simpleOption").contains(item.getType())){
                sFormatter.put(i.getCode(),
                        item.getLabel().trim() +" as string from the following options: "+item.getOptions()
                );
            }
        });
    }

//    public final float[][][][] SHAPE_INPUT_4 = new float[][][][]{};

    Map<Long, ZooModel> zooModelMap = new HashMap<>();

    public List<ImagePredict> classifyImg(Long cognaId, String imageDir, String fileName) throws OrtException {
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(()->new ResourceNotFoundException("Cogna","id",cognaId));

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
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, Helper.processImage(imageDir+"/"+fileName,1,3,224,224));
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
                    predictions2.add(new ImagePredict(classes.get(i), i, floatBuffer[0][i],0,0,0,0));
                }
            }

            predictions2.sort(Comparator.comparingDouble(a->-a.score()));
            predictions2 = predictions2.stream().filter(p->p.score()>minScore).toList();
            if (predictions2.size()>0) {
                predictions2 = predictions2.subList(0, limit);
            }

            return predictions2;
        } else {
            System.out.println("Failed to predict!");
            return List.of();
        }
    }


    public DetectedObjects detectImgNew(Long cognaId, String imageDir, String fileName) throws IOException, ModelException, TranslateException {

        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(()->new ResourceNotFoundException("Cogna","id",cognaId));

        if (!"imgcls".equals(cogna.getType())) throw new RuntimeException("Specified cogna is not image classifier");

        Path imageFile = Paths.get(imageDir+"/"+fileName);
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
        Cogna cogna = cognaRepository.findById(cognaId).orElseThrow(()->new ResourceNotFoundException("Cogna","id",cognaId));

        if (!"imgcls".equals(cogna.getType())) throw new RuntimeException("Specified cogna is not image classifier");

        String fullPath = modelPath + "/" + cogna.getInferModelName();
        int limit = cogna.getData().at("/imgclsLimit").asInt(1);
        double minScore = cogna.getData().at("/imgclsMinScore").asDouble(8.00);

        System.out.println("model:"+cogna.getInferModelName());

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

        Path imageFile = Paths.get(imageDir+"/"+fileName);
        ai.djl.modality.cv.Image img = ImageFactory.getInstance().fromFile(imageFile);
//
//        OnnxTensor.createTensor(env, ai.djl.modality.cv.Image)
        BufferedImage bi = Helper.processBufferedImageYolo(imageDir+"/"+fileName,640,640);

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, Helper.convertToFloatBuffer(bi,1,3,640,640));

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

            for (int i=0; i<floatBuffer[0].length; i++){
                float x0 = floatBuffer[0][i][0];
                float y0 = floatBuffer[0][i][1];
                float x1 = floatBuffer[0][i][2];
                float y1 = floatBuffer[0][i][3];
                float confidence = floatBuffer[0][i][4];
                int label = (int)floatBuffer[0][i][5];
                if (confidence>=minScore){

                    FontMetrics metrics = graph.getFontMetrics();

                    graph.setColor(Color.BLACK);

                    graph.drawRect((int)x0, (int)y0, (int)(x1-x0), (int)(y1-y0));

                    int lWidth = metrics.stringWidth(classes.get(label)) + 8;
                    int lHeight = metrics.getHeight();
                    graph.fillRect((int)x0, (int)y0 - lHeight, lWidth, lHeight);

                    graph.setColor(Color.WHITE);
                    graph.drawString(classes.get(label),(int)x0+4, (int)y0-4);

                    predictions2.add(new ImagePredict(classes.get(label),label, confidence, x0, y0, x1, y1));
                    System.out.println(">>>RESULT["+i+"]:"+x0+","+y0+","+x1+","+y1+","+classes.get(label)+","+confidence);
                }

            }
            graph.dispose();

            try {
                ImageIO.write(bi, "png",
                        new File(imageDir+"/segmented-"+fileName));
                System.out.println("writing image file");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return predictions2;
        } else {
            System.out.println("Failed to predict!");
            return List.of();
        }
    }
    Map<String, OrtSession> ortSessionMap = new HashMap<>();
    public record ImagePredict(String desc, int index, double score, double x1, double y1, double x2, double y2){}



}
