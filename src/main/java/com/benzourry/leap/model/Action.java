package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
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
@Table(name="ACTION")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Action {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "LABEL")
    String label;

    @Column(name = "TYPE", length = 25)
    String type; //[navigate, save, delete]

    @Column(name = "NEXT_TYPE", length = 250)
    String nextType;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "NEXT")
    Long next;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "PARAMS", length = 2000)
    String params;

    @Column(name = "F", length = 5000, columnDefinition = "text")
    String f;

    @JoinColumn(name = "SCREEN", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("screen-action")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Screen screen;

    @Transient
    private String fEncoded;

    @PostLoad
    private void optimizeJs() {
        if (this.f != null) {
            this.fEncoded = Helper.encodeBase64(
                    Helper.optimizeJs(this.f),
                    '@'
            );
        }
    }

    public String get_f() {
        return this.fEncoded;
    }

}
