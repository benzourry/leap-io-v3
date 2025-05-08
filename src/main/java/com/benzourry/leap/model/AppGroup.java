package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="APP_GROUP")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class AppGroup extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

//    @Column(name = "CODE")
//    String code;

    @Column(name = "NAME", length = 5000, columnDefinition = "text")
    String name;

    @Column(name = "DESCRIPTION", length = 5000, columnDefinition = "text")
    String description;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "ENABLED")
    private Integer enabled;

//    @Column(name = "ORDERING")
//    private Long ordering;

    @Column(name = "MANAGERS", length = 4000)
    String managers;


//    @JsonIgnore
//    @JoinColumn(name = "LOOKUP", referencedColumnName = "ID")
//    @ManyToOne(optional = false)
//    @OnDelete(action = OnDeleteAction.CASCADE)
//    Lookup lookup;
//
//    @Column(name = "LOOKUP",insertable=false, updatable=false)
//    Long lookupId;


}
