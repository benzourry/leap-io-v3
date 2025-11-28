package com.benzourry.leap.model;

import com.benzourry.leap.utility.LongListToStringConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name="DASHBOARD")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dashboard implements Serializable {

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


    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;


    @Column(name = "ACCESS_LIST")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> accessList;



    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "WIDE")
    boolean wide;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dashboard", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("dashboard-chart")
    @OrderBy("sortOrder ASC")
    Set<Chart> charts;


    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

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
//            } catch (NumberFormatException ignored) {
//            }
//        }
//        return result;
//    }

}
