package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

@Setter
@Getter
@Entity
@Table(name="COGNA_MCP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CognaMcp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

//    @Column(name = "DESCRIPTION")
//    String description;

    @Column(name = "SSE_URL")
    String sseUrl;

    @Column(name = "URL")
    String url;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode params;


    @Column(name = "TIMEOUT")
    int timeout=60; //10025


    @Column(name = "ENABLED")
    boolean enabled;


    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("cogna-mcp")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Cogna cogna;


}
