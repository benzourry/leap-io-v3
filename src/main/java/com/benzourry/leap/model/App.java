package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Entity
@Table(name="APP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class App extends BaseEntity implements Serializable {

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

    @Column(name = "CAN_PUSH")
    boolean canPush;

    @Column(name = "LIVE")
    boolean live;

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
    @Transient
    private String fEncoded;

    @Transient
    private String xEncoded;

    @PostLoad
    private void postLoadProcess() {

        // Prepare encoded F
        if (this.f != null) {
            this.fEncoded = Helper.encodeBase64(
                    Helper.optimizeJs(this.f),
                    '@'
            );
        }

        // Prepare encoded X
        if (this.x != null) {
            try {
                Map<String, Object> data = Helper.MAPPER.convertValue(this.x, HashMap.class);

                if (this.x.has("welcomeText")) {
                    data.put("welcomeText", Helper.optimizeJs(this.x.get("welcomeText").asText()));
                }

                String json = Helper.MAPPER.writeValueAsString(data);
                this.xEncoded = Helper.encodeBase64(json, '@');

            } catch (Exception ignored) {
                this.xEncoded = null;
            }
        }
    }

    public String get_f() {
        return this.fEncoded;
    }

    public String get_x() {
        return this.xEncoded;
    }

}
