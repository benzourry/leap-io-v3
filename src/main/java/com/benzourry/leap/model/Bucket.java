package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name="BUCKET")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

//    @Size(max = 512)
//    @NotNull
//    @Column(name = "CONFIG_VALUE")
//    String configValue;

//    String type; // adhoc, scheduled

//    String freq; // everyday, everyweek, everymonth;

//    String clock; //everyday:0101
//    Integer dayOfWeek; //everyweek:1,2,3,4,5,6,7
//    Integer dayOfMonth; // 25
//    Integer monthOfYear; // 2

//    String type; //mailblast

//    Long datasetId;

//    Long mailerId;

    @Column(name = "EMAIL")
    String email;

    @Column(name = "CODE")
    String code;


//    @Column(name = "INCLUDE_APP")
//    boolean includeApp;
//
//    @Column(name = "INCLUDE_USERS")
//    boolean includeUsers;
//
//    @Column(name = "INCLUDE_ENTRY")
//    boolean includeEntry;

//    @Column(name = "ENABLED")
//    private Integer enabled;

    @Column(name = "TIMESTAMP")
    Date timestamp;

//    @Type(type = "json")
//    @Column(columnDefinition = "json", name="SUMMARY")
//    private JsonNode summary;


    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "APP_NAME")
    String appName;

//
//    @JoinColumn(name = "APP", referencedColumnName = "ID")
//    @ManyToOne(optional = false)
//    App app;
}

