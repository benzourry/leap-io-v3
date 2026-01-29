package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Setter
@Getter
@Entity
@Table(name="BUCKET", indexes = {
        @Index(name = "idx_bucket_app_id", columnList = "APP_ID")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bucket extends Schedulable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

    @Column(name = "EMAIL")
    String email;

    @Column(name = "CODE")
    String code;

    @Column(name = "TIMESTAMP")
    Date timestamp;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "APP_NAME")
    String appName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode x;

}

