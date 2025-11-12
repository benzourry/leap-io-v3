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
@Table(name="COGNA_TOOL")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CognaTool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode params;


    @Column(name = "LAMBDA_ID")
    Long lambdaId; //10025 -- id of the component


    @Column(name = "ENABLED")
    boolean enabled;


    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("cogna-tool")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Cogna cogna;


}
