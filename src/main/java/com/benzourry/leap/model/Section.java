package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name="SECTION")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Section implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "SIZE")
    String size;

    @Column(name = "ALIGN")
    String align;

    @Column(name = "STYLE")
    String style;

    @Column(name = "CODE", length = 2000)
    String code;

    @Column(name = "TYPE")
    String type;

    @Column(name = "DESCRIPTION", length = 5000, columnDefinition = "text")
    String description;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "MAX_CHILD")
    Long maxChild;

    @Column(name = "MAIN")
    boolean main;

    @Column(name = "HIDE_HEADER")
    boolean hideHeader;

    @Column(name = "HIDDEN")
    boolean hidden;

    @Column(name = "INLINE")
    boolean inline;

    @Column(name = "ORDERABLE")
    boolean orderable;

    @Column(name = "CONFIRMABLE")
    boolean confirmable;

    @Column(name = "FOR_APPROVAL")
    boolean forApproval;

    @Column(name = "PRE", length = 2000)
    String pre;

    @Column(name = "ENABLED_FOR", length = 2000)
    String enabledFor;

    @Column(name = "ICON", length = 100)
    String icon;

    @Column(name = "ADD_LABEL", length = 100)
    String addLabel;

    @Column(name = "PARENT")
    Long parent;

    @Column(name = "DATASET_ID")
    Long datasetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "section", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("section-item")
    @OrderBy("sortOrder ASC")
    Set<SectionItem> items;

    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("form-section")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;

    public String get_pre(){
        return Helper.encodeBase64(Helper.optimizeJs(this.pre),'@');
    }

}
