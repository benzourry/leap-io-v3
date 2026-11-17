package com.benzourry.leap.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "USER_ASSISTANT")
@Getter
@Setter
public class AssistantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_EMAIL", nullable = false, length = 320)
    private String userEmail;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "API_BASE_URL", nullable = false, length = 1000)
    private String apiBaseUrl;

    @Column(name = "MODEL", nullable = false, length = 200)
    private String model;

    @Column(name = "API_KEY", nullable = false, columnDefinition = "text")
    private String encryptedApiKey;

    @Column(name = "ADDITIONAL_INSTRUCTIONS", columnDefinition = "text")
    private String additionalInstructions = "";

    @Column(name = "MAX_ACTIONS", nullable = false)
    private Integer maxActions = 60;

    @Column(name = "IS_SELECTED", nullable = false)
    private boolean selected;
}
