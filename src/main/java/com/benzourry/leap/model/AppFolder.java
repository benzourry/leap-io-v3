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
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name="APP_GROUP", indexes = {
        @Index(name = "idx_app_folder_email", columnList = "EMAIL")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class AppFolder extends BaseEntity implements Serializable {

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

//    @Column(name = "ENABLED")
//    private Integer enabled;

    @Column(name = "EMAIL", length = 4000)
    String email;


    // --- ADD THIS MANY-TO-MANY RELATIONSHIP ---
    @ManyToMany
    @JoinTable(
            name = "APP_FOLDER_MAPPING", // The join table that JPA will automatically create
            joinColumns = @JoinColumn(name = "FOLDER_ID"),
            inverseJoinColumns = @JoinColumn(name = "APP_ID"),
            indexes = {
                    @Index(name = "idx_afm_folder", columnList = "FOLDER_ID"),
                    @Index(name = "idx_afm_app", columnList = "APP_ID")
            }
    )
    private List<App> apps = new ArrayList<>();

}
