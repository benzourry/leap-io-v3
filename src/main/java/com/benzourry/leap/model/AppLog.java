package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;

@Setter
@Getter
@Entity
@Table(name="APP_LOG", indexes = {
        @Index(name = "idx_app_log_app_id", columnList = "APP_ID"),
        @Index(name = "idx_app_log_exec_id", columnList = "EXEC_ID"),
        @Index(name = "idx_app_log_status", columnList = "STATUS"),
        @Index(name = "idx_app_log_module", columnList = "MODULE"),
        @Index(name = "idx_app_log_module_id", columnList = "MODULE_ID"),
        @Index(name = "idx_app_log_email", columnList = "EMAIL")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppLog {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "APP_ID")
    private Long appId;           // Crucial for isolation!

    @Column(name = "EXEC_ID")
    private String execId;   // e.g., UUID for a specific run

    @Column(name = "STATUS")
    private String status;        // SUCCESS or FAILED

    @Column(name = "MODULE")
    private String module; // lambda, cogna, entry

    @Column(name = "MODULE_ID")
    private Long moduleId;

    @Column(name = "DATA", length = 5000, columnDefinition = "text")
//    @Lob // Store the logs as a JSON string or text block
    private String data;

    @Column(name = "EMAIL")
    private String email; // lambda, cogna, entry

    @Column(name = "TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
}
