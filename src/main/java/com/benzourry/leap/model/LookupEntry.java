package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="LOOKUP_ENTRY")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class LookupEntry extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "CODE")
    String code;

    @Column(name = "NAME", length = 5000, columnDefinition = "text")
    String name;

    @Column(name = "EXTRA", length = 5000, columnDefinition = "text")
    String extra;

    @JdbcTypeCode(SqlTypes.JSON)
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

    @Column(name = "LOOKUP",insertable=false, updatable=false)
    Long lookupId;


}
