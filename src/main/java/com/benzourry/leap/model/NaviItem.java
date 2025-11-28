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

    @Type(value = JsonType.class)
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
