package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Setter
@Getter
@Entity
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name="META_TRAIL", indexes = {
        @Index(name = "idx_meta_trail_app_id", columnList = "APP_ID"),
        @Index(name = "idx_meta_trail_action", columnList = "ACTION"),
        @Index(name = "idx_meta_trail_scope", columnList = "SCOPE"),
        @Index(name = "idx_meta_trail_module", columnList = "MODULE"),
        @Index(name = "idx_meta_trail_module_id", columnList = "MODULE_ID"),
        @Index(name = "idx_meta_trail_email", columnList = "EMAIL")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaTrail {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "APP_ID")
    private Long appId;           // Crucial for isolation!

    @Column(name = "ACTION")
    private String action;   // e.g., UUID for a specific run

    @Column(name = "SCOPE")
    private String scope;        // SUCCESS or FAILED

    @Column(name = "MODULE")
    private String module; // lambda, cogna, entry

    @Column(name = "MODULE_ID")
    private Long moduleId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode data;

//    @CreatedBy
    @Column(name = "EMAIL", updatable = false)
    private String email;

    @CreatedDate // 2. ADD THIS
    @Column(name = "TIMESTAMP", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public MetaTrail() {
//        this.timestamp = new Date();
    }

}
