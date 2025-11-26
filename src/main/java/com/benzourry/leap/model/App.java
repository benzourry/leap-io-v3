package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Entity
@Table(name="APP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class App extends BaseEntity implements Serializable {

    // Reuse a single ObjectMapper instance
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE", length = 4000)
    String title;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

//    @Column(name = "PUBLISHED")
//    boolean published;

    @Column(name = "EMAIL", length = 4000)
    String email;

    @Column(name = "LOGO")
    String logo;

    @Column(name = "APP_PATH")
    String appPath;

    @Column(name = "APP_DOMAIN")
    String appDomain;

    @Column(name = "LAYOUT")
    String layout;

    @Column(name = "THEME")
    String theme;

    @Column(name = "STATUS")
    String status; // LOCAL,TEMPLATE, PUBLISHED

    @Column(name = "TAG")
    String tag;

    @Column(name = "START_PAGE")
    String startPage;

    @Column(name = "ONCE")
    Long once;

    @Column(name = "CLONE")
    Long clone;

    @Column(name = "USE_GOOGLE")
    boolean useGoogle;

    @Column(name = "USE_UNIMAS")
    boolean useUnimas;

    @Column(name = "USE_UNIMASID")
    boolean useUnimasid;

    @Column(name = "USE_ICATSID")
    boolean useIcatsid;

    @Column(name = "USE_SSONE")
    boolean useSsone;

    @Column(name = "USE_FACEBOOK")
    boolean useFacebook;

    @Column(name = "USE_GITHUB")
    boolean useGithub;

    @Column(name = "USE_AZUREAD")
    boolean useAzuread;

    @Column(name = "USE_TWITTER")
    boolean useTwitter;

    @Column(name = "USE_LINKEDIN")
    boolean useLinkedin;

    @Column(name = "USE_SARAWAKID")
    boolean useSarawakid;

    @Column(name = "USE_MYDID")
    boolean useMyDID;

    @Column(name = "USE_EMAIL")
    boolean useEmail;

    @Column(name = "USE_ANON")
    boolean useAnon;

//    @Column(name = "PUBLIC_ACCESS")
//    boolean publicAccess;

    @Column(name = "CAN_PUSH")
    boolean canPush;

    @Column(name = "LIVE")
    boolean live;



//    @Column(name = "SHARED")
//    boolean shared;
//
//    @Column(name = "SECRET")
//    boolean secret;

//    @Column(name = "BLOCK_ANON")
//    boolean blockAnon;


    @JoinColumn(name = "APP_GROUP", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    AppGroup group;


    @Column(name = "REG")
    Boolean reg;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "F", length = 5000, columnDefinition = "text")
    String f;

    public String get_f(){
        return Helper.encodeBase64(Helper.optimizeJs(this.f),'@');
    }

    public String get_x(){

        if (this.getX()==null) return null;

        Map<String, Object> data = MAPPER.convertValue(this.getX(), HashMap.class);

        data.put("welcomeText",Helper.optimizeJs(this.getX().at("/welcomeText").asText()));

        String json = "";
        try {
            json = MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
        }

//        return Helper.encodeBase64(this.dataText);
        return Helper.encodeBase64(json,'@');
    }


//    @Type(type = "json")
//    @Column(columnDefinition = "json")
//    JsonNode navi;

//    @JoinColumn(name = "NAVI_OBJ", referencedColumnName = "ID")
//    @OneToOne(cascade = CascadeType.ALL)
//    Navi naviObj;

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "app", fetch = FetchType.LAZY)
//    @JsonManagedReference("app-group")
//    @OrderBy("sortOrder ASC")
//    List<NaviGroup> navis = new ArrayList<>();

}
