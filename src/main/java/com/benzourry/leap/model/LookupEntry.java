package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="LOOKUP_ENTRY")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LookupEntry extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "CODE")
    String code;

    @Column(name = "NAME")
    String name;

    @Column(name = "EXTRA")
    String extra;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode data;

    @Column(name = "ENABLED")
    private Integer enabled;

    @Column(name = "ORDERING")
    private Long ordering;

    @JsonIgnore
    @JoinColumn(name = "LOOKUP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Lookup lookup;
}
