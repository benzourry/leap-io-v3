package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.io.Serializable;

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

    @Column(name = "EMAIL")
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

//    @Column(name = "PRICE")
//    Double price;

    @Column(name = "CLONE")
    Long clone;

    @Column(name = "USE_GOOGLE")
    boolean useGoogle;

    @Column(name = "USE_UNIMAS")
    boolean useUnimas;

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

    @Column(name = "USE_EMAIL")
    boolean useEmail;

    @Column(name = "USE_ANON")
    boolean useAnon;

    @Column(name = "PUBLIC_ACCESS")
    boolean publicAccess;

    @Column(name = "CAN_PUSH")
    boolean canPush;

//    @Column(name = "SHARED")
//    boolean shared;
//
//    @Column(name = "SECRET")
//    boolean secret;

//    @Column(name = "BLOCK_ANON")
//    boolean blockAnon;

    @Column(name = "REG")
    Boolean reg;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "F", length = 5000, columnDefinition = "text")
    String f;

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
