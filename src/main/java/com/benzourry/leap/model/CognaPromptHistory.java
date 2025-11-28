package com.benzourry.leap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Setter
@Getter
@Entity
@ToString
@Table(name="COGNA_PROMPT_HISTORY")
public class CognaPromptHistory {

    public static final String BY_LLM = "llm";
    public static final String BY_USER = "user";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "EMAIL")
    String email;

    @Column(name = "TEXT", columnDefinition = "text")
    String text;

    @Column(name = "RESPONSE", columnDefinition = "text")
    String response;

    @Column(name = "TYPE")
    String type; // llm,user

    @Column(name = "TIMESTAMP")
    Date timestamp; // llm,user

    @Column(name = "COGNA_ID")
    Long cognaId;

    public CognaPromptHistory(){}

    public CognaPromptHistory(Long cognaId, String text, String response, String email, String type){
        this.cognaId = cognaId;
        this.text = text;
        this.response = response;
        this.email = email;
        this.type = type;
        this.timestamp = new Date();
    }

}
