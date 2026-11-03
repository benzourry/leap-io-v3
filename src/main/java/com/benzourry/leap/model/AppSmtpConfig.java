package com.benzourry.leap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "APP_SMTP_CONFIG")
@Getter
@Setter
public class AppSmtpConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "HOST", nullable = false)
    private String host;

    @Column(name = "PORT", nullable = false)
    private Integer port = 587;

    @Column(name = "USERNAME")
    private String username;

    // Use a JPA AttributeConverter to automatically encrypt/decrypt this column
    @Column(name = "PASSWORD")
    // @Convert(converter = StringCryptoConverter.class)
    private String password;

    @Column(name = "USE_AUTH")
    private boolean auth = true;

    @Column(name = "USE_STARTTLS")
    private boolean startTls = true;

    @Column(name = "SENDER_EMAIL")
    private String senderEmail; // Crucial: Custom SMTPs often require a matching 'From' address

    @Column(name = "ENABLED")
    private boolean enabled = true;

    @Column(name = "APP_ID")
    private Long appId;           // Crucial for isolation!
}