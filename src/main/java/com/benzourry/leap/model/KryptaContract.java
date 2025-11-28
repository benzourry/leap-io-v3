package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "krypta_contract")
@Setter
@Getter
public class KryptaContract {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    @Column(name = "SOL", length = 5000, columnDefinition = "text")
    private String sol;

    private String abi;

    private String bin;

    private String email;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode abiSummary;

    @JsonIgnore
    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

}
