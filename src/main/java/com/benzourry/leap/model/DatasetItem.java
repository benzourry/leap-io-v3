package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="DATASET_ITEM")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasetItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "LABEL", length = 2000)
    String label;

    @Column(name = "CODE", length = 2000)
    String code;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "FORM_ID")
    Long formId;

    @Column(name = "ROOT")
    String root;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    JsonNode subs;

    @Column(name = "PREFIX")
    String prefix;

    @Column(name = "TYPE") //section,list,approval
    String type;

    @Column(name = "PRE", length = 2000)
    String pre;

    @JoinColumn(name = "DATASET", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("dataset-item")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Dataset dataset;

    // Cache for optimized Base64 + JS result
    @Transient
    private String cachedPre;

    public String get_pre() {
        if (pre == null) return null;
        if (cachedPre == null) {
            cachedPre = Helper.encodeBase64(Helper.optimizeJs(pre), '@');
        }
        return cachedPre;
    }

    // Optional: reset cache if f or pre is updated
    public void setPre(String pre) {
        this.pre = pre;
        this.cachedPre = null;
    }

}
