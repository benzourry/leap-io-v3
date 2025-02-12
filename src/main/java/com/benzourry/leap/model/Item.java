package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name="ITEM")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "LABEL", length = 250)
    String label;

    @Column(name = "CODE", length = 250)
    String code;

    @Column(name = "TYPE", length = 500)
    String type;

    @Column(name = "SUB_TYPE", length = 500)
    String subType;

    @Column(name = "HINT", length = 2000)
    String hint;

    @Column(name = "PRE", length = 2000)
    String pre;

    @Column(name = "POST", length = 5000, columnDefinition = "text")
    String post;

    @Column(name = "PLACEHOLDER", length = 5000, columnDefinition = "text")
    String placeholder;

    @Column(name = "F", length = 5000, columnDefinition = "text")
    String f;

    @Column(name = "SIZE")
    String size;

    @Column(name = "DATASOURCE")
    Long dataSource;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode dataSourceInit;

    @Column(name = "OPTIONS", length = 4000)
    String options;

    @Column(name = "FORMAT")
    String format;

    @Column(name = "BIND_LABEL")
    String bindLabel; // Map which field to be displayed on lookup or modelPicker

//    @Column(name = "SORT_ORDER")
//    Long sortOrder;

//    @Column(name = "POST_BTN")
//    boolean postBtn;
//
//    @Column(name = "POST_BTN_LBL")
//    String postBtnLbl; // Map which field to be displayed on lookup or modelPicker

    @Column(name = "HIDE_LABEL")
    boolean hideLabel;

    @Column(name = "HIDDEN")
    boolean hidden;

    @Column(name = "READ_ONLY")
    boolean readOnly;

    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("form-items")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;

//    @ElementCollection(fetch = FetchType.LAZY)
//    @CollectionTable(
//            name="FORM_ITEM_V",
//            joinColumns=@JoinColumn(name="FORM_ITEM_ID")
//    )
//    @Column(name="V", length = 4000)
//    @MapKeyColumn(name="K")
//    private Map<String, String> v = new HashMap<String, String>();

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode facet; // {"facet":"mode"} xjd pake, dlm x jk

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode v;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

}
