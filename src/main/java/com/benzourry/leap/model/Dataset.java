package com.benzourry.leap.model;

import com.benzourry.leap.utility.LongListToStringConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

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

    @Column(name = "ACCESS_LIST")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> accessList;

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

    @Column(name = "Q_FILTER", columnDefinition = "TEXT")
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

    // --- AccessList optimized ---
//    public void setAccessList(List<Long> val) {
//        if (val == null || val.isEmpty()) {
//            this.accessList = null;
//        } else {
//            StringBuilder sb = new StringBuilder();
//            for (int i = 0; i < val.size(); i++) {
//                if (i > 0) sb.append(',');
//                sb.append(val.get(i));
//            }
//            this.accessList = sb.toString();
//        }
//    }
//
//    public List<Long> getAccessList() {
//        if (Helper.isNullOrEmpty(this.accessList)) return Collections.emptyList();
//        String[] parts = this.accessList.split(",");
//        List<Long> result = new ArrayList<>(parts.length);
//        for (String p : parts) {
//            try {
//                result.add(Long.parseLong(p));
//            } catch (NumberFormatException ignored) {}
//        }
//        return result;
//    }

}
