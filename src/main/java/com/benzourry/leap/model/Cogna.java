package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

//import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_VISION_PREVIEW;

@Setter
@Getter
@Entity
@Table(name="COGNA")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Cogna extends BaseEntity implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

    @Column(name = "TYPE")
    String type; // textgen, imagegen, textclass



    @Column(name = "EMBED_MODEL_TYPE")
    String embedModelType; // OpenAI, HuggingFace

    @Column(name = "EMBED_MODEL_NAME")
    String embedModelName; // [OpenAI=>[text-embedding-ada-002,], HuggingFace=>[allMiniLML6v2]]

    @Column(name = "EMBED_MODEL_API_KEY")
    String embedModelApiKey; // [OpenAI=>[text-embedding-ada-002,], HuggingFace=>[allMiniLML6v2]]

    @Column(name = "EMBED_MAX_RESULT")
    Integer embedMaxResult; // [OpenAI=>[text-embedding-ada-002,], HuggingFace=>[allMiniLML6v2]]

    @Column(name = "EMBED_MIN_SCORE")
    Double embedMinScore; // [OpenAI=>[text-embedding-ada-002,], HuggingFace=>[allMiniLML6v2]]

    @Column(name = "MAX_CHAT_MEMORY")
    Integer maxChatMemory; // [OpenAI=>[text-embedding-ada-002,], HuggingFace=>[allMiniLML6v2]]

    @Column(name = "MAX_TOKEN")
    Integer maxToken; // [OpenAI=>[text-embedding-ada-002,], HuggingFace=>[allMiniLML6v2]]

    @Column(name = "CHUNK_LENGTH")
    Integer chunkLength;

    @Column(name = "CHUNK_OVERLAP")
    Integer chunkOverlap;

    @Column(name = "INFER_MODEL_TYPE")
    String inferModelType; // OpenAI, HuggingFace

    @Column(name = "INFER_MODEL_NAME")
    String inferModelName; // [OpenAI=>[gpt3.5turbo,], HuggingFace=>[llama]]

    @Column(name = "INFER_MODEL_API_KEY")
    String inferModelApiKey; // [OpenAI=>[gpt3.5turbo,], HuggingFace=>[llama]]

    @Column(name = "VECTOR_STORE_TYPE")
    String vectorStoreType; // [OpenAI=>[gpt3.5turbo,], HuggingFace=>[llama]]

    @Column(name = "VECTOR_STORE_HOST")
    String vectorStoreHost; // [OpenAI=>[gpt3.5turbo,], HuggingFace=>[llama]]

    @Column(name = "VECTOR_STORE_PORT")
    Long vectorStorePort; // [OpenAI=>[gpt3.5turbo,], HuggingFace=>[llama]]

    @Column(name = "VECTOR_STORE_DIM")
    Long vectorStoreDim; // [OpenAI=>[gpt3.5turbo,], HuggingFace=>[llama]]

    @Column(name = "TEMPERATURE")
    Double temperature;

    @Column(name = "SYSTEM_MESSAGE", length = 5000, columnDefinition = "text")
    String systemMessage;

    @Column(name = "POST_MESSAGE", length = 2000)
    String postMessage;

//    @Column(name = "API_KEY")
//    String apiKey;



//    @Column(name = "MODEL_TYPE")
//    String modelType; // [GPT4All, LlamaCpp]
//
//    @Column(name = "MODEL_PATH")
//    String modelPath; // [GPT4All, LlamaCpp]

    @Column(name = "EMAIL")
    String email;

    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    UserGroup access;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode data;

//    @Column(name = "SCHEDULED")
//    boolean scheduled;
//
//    @Column(name = "FREQ")
//    String freq; // everyday, everyweek, everymonth;
//
//    @Column(name = "CLOCK")
//    String clock; //everyday:0101
//
    @Column(name = "CODE")
    String code; //unique


    @Column(name = "AUGMENTOR")
    String augmentor;

    @Column(name = "MM_SUPPORT")
    Boolean mmSupport;
//
//    @Column(name = "DAY_OF_WEEK")
//    Integer dayOfWeek; //everyweek:1,2,3,4,5,6,7
//
//    @Column(name = "DAY_OF_MONTH")
//    Integer dayOfMonth; // 25
//
//    @Column(name = "MONTH_OF_YEAR")
//    Integer monthOfYear; // 2

    @Column(name = "PUBLIC_ACCESS")
    boolean publicAccess;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cogna", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("cogna-source")
    @OrderBy("id ASC")
    private Set<CognaSource> sources = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cogna", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("cogna-tool")
    @OrderBy("id ASC")
    private Set<CognaTool> tools = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cogna", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("cogna-mcp")
    @OrderBy("id ASC")
    private Set<CognaMcp> mcps = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cogna", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("cogna-sub")
    @OrderBy("id ASC")
    private Set<CognaSub> subs = new HashSet<>();

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    public boolean isStreamSupport(){
        return ("deepseek".equals(this.inferModelType))||
                ("ollama".equals(this.inferModelType))||
                ("openai".equals(this.inferModelType)
                && !Helper.isNullOrEmpty(this.inferModelApiKey)
                && !"demo".equals(this.inferModelApiKey)) ||
                ("vertex-ai-gemini".equals(this.inferModelType))||
                ("gemini".equals(this.inferModelType))||
                ("localai".equals(this.inferModelType));
    }
//    public boolean isMmSupport(){
//        return (GPT_4_VISION_PREVIEW.equals(this.getInferModelName()) ||
//                ("gemini-pro".equals(this.getInferModelName())));
//    }
}
