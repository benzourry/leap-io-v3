package com.benzourry.leap.model;

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

@Setter
@Getter
@Entity
@Table(name="NAVI_ITEM")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NaviItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String title;
    String type;

    @Column(name = "SCREEN_ID")
    Long screenId;

    String url;

    String icon;

    boolean fl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "APP_ID")
    Long appId;

    String pre;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @JoinColumn(name = "NAVI_GROUP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("group-item")
    @OnDelete(action = OnDeleteAction.CASCADE)
    NaviGroup group;

}
