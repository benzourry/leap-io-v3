package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    /***
     * PENTING UTK MIGRATION !!!!!
     */
//    @Type(value = JsonType.class)
//    @Column(columnDefinition = "json")
//    private JsonNode screen;
//
//    @Type(value = JsonType.class)
//    @Column(columnDefinition = "json")
//    private JsonNode next;
//
//    @Type(value = JsonType.class)
//    @Column(columnDefinition = "json")
//    private JsonNode facet; // {facet:"<button label>"}
//    @Column(name = "CAN_VIEW")
//    boolean canView;
//
//    @Column(name = "CAN_EDIT")
//    boolean canEdit;
//
//    @Column(name = "CAN_RETRACT")
//    boolean canRetract;
//
//    @Column(name = "CAN_DELETE")
//    boolean canDelete;
//
//    @Column(name = "CAN_RESET")
//    boolean canReset;
//
//    @Column(name = "CAN_APPROVE")
//    boolean canApprove;

    @Column(name = "STATUS")
    String status;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json", name="STATUS_FILTER")
    JsonNode statusFilter;


    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "CAN_BLAST")
    boolean canBlast;

    @Column(name = "BLAST_TO")
    String blastTo;

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


    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

//    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
//    @ManyToOne
//    @NotFound(action = NotFoundAction.IGNORE)
//    @OnDelete(action = OnDeleteAction.NO_ACTION)
//    UserGroup access;

    @Column(name = "ACCESS_LIST")
    String accessList;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dataset", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("dataset-item")
    @OrderBy("sortOrder ASC")
    List<DatasetItem> items;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dataset", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("dataset-action")
    @OrderBy("sortOrder ASC")
    List<DatasetAction> actions;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dataset", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("dataset-filter")
    @OrderBy("sortOrder ASC")
    Set<DatasetFilter> filters;


    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode presetFilters;

//    @Type(value = JsonType.class)
//    @Column(columnDefinition = "json")
    String qFilter;


    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

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

    public void setAccessList(List<Long> val){
        if (!Helper.isNullOrEmpty(val)) {
            this.accessList = val.stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
    }

    public List<Long> getAccessList(){
        if (!Helper.isNullOrEmpty(this.accessList)) {
            return Arrays.asList(this.accessList.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }


}
