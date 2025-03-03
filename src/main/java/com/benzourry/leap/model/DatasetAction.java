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

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="DATASET_ACTION")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasetAction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "LABEL", length = 2000)
    String label;

    @Column(name = "TYPE") //inline,dropdown, bulk
    String type;

    @Column(name = "STYLE") //btn-secondary, btn-success, btn-danger
    String style;

    @Column(name = "ICON", length = 100)
    String icon;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "INPOP")
    boolean inpop;

    @Column(name = "ACTION") //view,edit,delete,prev,facet,screen,function, url
    String action;

    @Column(name = "PARAMS") //view,edit,delete,prev,facet,screen,function, url
    String params;

    @Column(name = "NEXT") //formId, facet, screenId
    String next;


    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "PRE", length = 2000)
    String pre;

    @Column(name = "F", length = 5000, columnDefinition = "text")
    String f;

    @Column(name = "URL") //section,list,approval
    String url;

    @JoinColumn(name = "DATASET", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("dataset-action")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Dataset dataset;

    public static final String ACTION_EDIT = "edit";
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_RETRACT = "retract";
    public static final String ACTION_APPROVE = "approve";
    public static final String ACTION_SCREEN = "screen";
    public static final String ACTION_FACET = "facet";
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_URL = "url";
    public static final String ACTION_FUNCTION = "function";
    public static final String TYPE_DROPDOWN = "dropdown";
    public static final String TYPE_INLINE = "inline";

    public DatasetAction(){}

    public DatasetAction(String label, String action, String type, boolean inpop, String icon, Long sortOrder, Dataset dataset){
        this.label = label;
        this.action = action;
        this.type = type;
        this.inpop = inpop;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.dataset = dataset;
    }

}
