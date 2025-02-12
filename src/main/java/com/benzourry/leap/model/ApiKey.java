package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name="API_KEY")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiKey extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "API_KEY")
    String apiKey;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "TIMESTAMP")
    Date timestamp;

}
