package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name="DATASET")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dataset extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "SIZE")
    String size;

    @Column(name = "CODE", length = 2000)
    String code;

    @Column(name = "TYPE")
    String type;

    @Column(name = "DESCRIPTION")
    String description;

    @Column(name = "UI")
    String ui;

    @Column(name = "UI_TEMPLATE")
    String uiTemplate;

    @Column(name = "DEFAULT_SORT")
    String defaultSort;

    @Column(name = "DEF_SORT_FIELD")
    String defSortField;

    @Column(name = "DEF_SORT_DIR")
    String defSortDir;


    @Column(name = "INPOP")
    String inpop;

//    @Column(name = "NEXT")
//    Long next;
//
//    @Column(name = "NEXT_LABEL")
//    String nextLabel;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode screen;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode next;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode facet; // {facet:"<button label>"}


    @Column(name = "STATUS")
    String status;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json", name="STATUS_FILTER")
    JsonNode statusFilter;


    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "CAN_VIEW")
    boolean canView;

    @Column(name = "CAN_EDIT")
    boolean canEdit;

    @Column(name = "CAN_RETRACT")
    boolean canRetract;

    @Column(name = "CAN_DELETE")
    boolean canDelete;

    @Column(name = "CAN_RESET")
    boolean canReset;

    @Column(name = "CAN_APPROVE")
    boolean canApprove;

    @Column(name = "CAN_BLAST")
    boolean canBlast;


    @Column(name = "BLAST_TO")
    String blastTo;

//    @Column(name = "CAN_NEXT")
//    boolean canNext;

//    @Column(name = "CAN_FILTER_STATUS")
//    boolean canFilterStatus;

    @Column(name = "SHOW_INDEX")
    boolean showIndex;

    @Column(name = "SHOW_STATUS")
    boolean showStatus;

    @Column(name = "WIDE")
    boolean wide;

    @Column(name = "SHOW_ACTION")
    boolean showAction;

    @Column(name = "EXPORT_XLS")
    boolean exportXls;

    @Column(name = "EXPORT_CSV")
    boolean exportCsv;

    @Column(name = "EXPORT_PDF")
    boolean exportPdf;

    @Column(name = "PUBLIC_EP")
    boolean publicEp;

    @Column(name = "EXPORT_PDF_LAYOUT")
    String exportPdfLayout; //a4, a4_landscape

//    @Column(name = "ADMIN_ONLY")
//    boolean adminOnly;

//    @Column(name = "ADMIN")
//    String admin;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    UserGroup access;

//    @Column(name = "PRE", length = 2000)
//    String pre;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dataset", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("dataset-item")
    @OrderBy("sortOrder ASC")
    List<DatasetItem> items;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dataset", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("dataset-filter")
    @OrderBy("sortOrder ASC")
    Set<DatasetFilter> filters;


    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode presetFilters;


    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

//
//    public void setFields(Set<String> arrList){
//        this.fields = String.join(";",arrList);
//    }
//
//    public Set<String> getFields() {
//        Set<String> arrList =  null;
//        if (fields != null){
//            arrList = new HashSet<>(Arrays.asList(fields.split(";")));
//        }
//        return arrList;
//    }


}
