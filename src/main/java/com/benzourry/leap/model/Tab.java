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

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="TAB")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tab implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "CODE")
    String code;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "PRE", length = 2000)
    String pre;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("form-tab")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;

    public String get_pre(){
        return Helper.encodeBase64(Helper.optimizeJs(this.pre),'@');
    }

}
