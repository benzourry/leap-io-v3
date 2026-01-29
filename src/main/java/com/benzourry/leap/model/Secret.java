package com.benzourry.leap.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Date;

@Setter
@Getter
@Entity
@DynamicUpdate
@Table(name = "SECRET", indexes = {
        @Index(name = "idx_secret_app_id", columnList = "APP_ID"),
        @Index(name = "idx_secret_key_app_id", columnList = "K, APP_ID")
})
public class  Secret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "K")
    private String key;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "V", length=4000)
    private String value;

    @Column(name = "ENABLED")
    private Integer enabled;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "TIMESTAMP")
    Date timestamp;

    public Secret(){}

    public Secret(String key, String value, Integer enabled){
        this.key = key;
        this.setValue(value);
        this.enabled = enabled;
    }

    public static Secret  of (String key, String value, Integer enabled){
        return new Secret(key, value, enabled);
    }
}
