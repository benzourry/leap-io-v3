package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="APP_GROUP", indexes = {
        @Index(name = "idx_app_group_managers", columnList = "MANAGERS")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class AppGroup extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME", length = 5000, columnDefinition = "text")
    String name;

    @Column(name = "DESCRIPTION", length = 5000, columnDefinition = "text")
    String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "ENABLED")
    private Integer enabled;

    @Column(name = "MANAGERS", length = 4000)
    String managers;

}
