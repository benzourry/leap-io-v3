package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.StringListToStringConverter;
import com.benzourry.leap.utility.audit.AuditableMetaEntity;
import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Entity
@Table(name="APP", indexes = {
        @Index(name = "idx_app_status", columnList = "STATUS"),
        @Index(name = "idx_app_path", columnList = "APP_PATH"),
        @Index(name = "idx_app_domain", columnList = "APP_DOMAIN")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class App extends AuditableMetaEntity implements Serializable, Trailable {

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

    /** Attempt to make Signin options dynamic **/
    @Column(name = "ENABLED_SIGNIN")
    @Convert(converter = StringListToStringConverter.class)
    private List<String> enabledSignin = new ArrayList<>();

    @Column(name = "USE_EMAIL")
    boolean useEmail;

    @Column(name = "USE_ANON")
    boolean useAnon;

    @Column(name = "CAN_PUSH")
    boolean canPush;

    @Column(name = "LIVE")
    boolean live;

    @JoinColumn(name = "APP_GROUP", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    AppGroup group;

    @ManyToMany(mappedBy = "apps")
    @JsonIgnore
    private List<AppFolder> folders = new ArrayList<>();


    @Column(name = "REG")
    Boolean reg;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode x;

    // Add this to your fields in App.java
    @Transient
    @JsonProperty("providerNames")
    private Map<String, String> providerNames;

    // Add Getter and Setter
    public Map<String, String> getProviderNames() {
        return providerNames;
    }

    public void setProviderNames(Map<String, String> providerNames) {
        this.providerNames = providerNames;
    }

    // Add this to your fields in App.java
    @Transient
    @JsonProperty("providerIcons")
    private Map<String, String> providerIcons;

    // Add Getter and Setter
    public Map<String, String> getProviderIcons() {
        return providerIcons;
    }

    public void setProviderIcons(Map<String, String> providerIcons) {
        this.providerIcons = providerIcons;
    }


    @Column(name = "F", length = 5000, columnDefinition = "text")
    String f;

    // Optimized get_f and get_pre with caching to reduce repeated Base64 + JS processing if needed
    @Transient
    private String cachedF;

    public String get_f() {
        if (f == null) return null;
        if (cachedF == null) {
            cachedF = Helper.encodeBase64(Helper.optimizeJs(f), '@');
        }
        return cachedF;
    }

    // Optional: reset cache if f or pre is updated
    public void setF(String f) {
        this.f = f;
        this.cachedF = null;
    }

    @Transient
    private String cachedX;

    public String get_x() {
        if (x == null) return null;
        try {
            Map<String, Object> data = Helper.MAPPER.convertValue(this.x, Map.class);

            if (this.x.has("welcomeText")) {
                data.put("welcomeText", Helper.optimizeJs(this.x.get("welcomeText").asText()));
            }

            String json = Helper.MAPPER.writeValueAsString(data);
            this.cachedX = Helper.encodeBase64(json, '@');

        } catch (Exception ignored) {
            this.cachedX = null;
        }
        return cachedX;
    }

    // Optional: reset cache if f or pre is updated
    public void setX(JsonNode x) {
        this.x = x;
        this.cachedX = null;
    }

    @Override
    public Long getTrailId() {
        return this.id;
    }

    @Override
    public String getTrailModule() {
        return "app";
    }

    @Override
    public String getTrailScope() {
        return "app_" + this.id;
    }

    @Override
    public Long getTrailAppId() {
        return this.id;
    }
}
