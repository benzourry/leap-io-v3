package com.benzourry.leap.model;


import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.LongListToStringConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name="SCREEN")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Screen extends BaseEntity implements Serializable{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

    @Column(name = "TYPE")
    String type; // [qr,search,view,form,list,]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    JsonNode data;

    @JsonIgnore
    @Column(name = "DATA",insertable=false, updatable=false)
    String dataText;

    @Column(name = "NEXT")
    Long next;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "SHOW_ACTION")
    boolean showAction;

    @Column(name = "CAN_PRINT")
    boolean canPrint;

    @Column(name = "WIDE")
    boolean wide;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "screen", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("screen-action")
    @OrderBy("id ASC")
    private Set<Action> actions = new HashSet<>();

    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Form form;

    @JoinColumn(name = "DATASET", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Dataset dataset;

    @JoinColumn(name = "BUCKET", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Bucket bucket;

    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Cogna cogna;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

    @Column(name = "ACCESS_LIST")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> accessList;

    public String get_data(){

        if (this.getData()==null) return null;

        String json = this.dataText;

        Map<String, Object> data = Helper.MAPPER.convertValue(this.getData(), Map.class);

        data.put("f", Helper.optimizeJs(this.getData().at("/f").asText()));
        data.put("content", Helper.optimizeHtml(this.getData().at("/content").asText()));
        data.put("pretext", Helper.optimizeHtml(this.getData().at("/pretext").asText()));
        data.put("posttext", Helper.optimizeHtml(this.getData().at("/posttext").asText()));

        try {
            json = Helper.MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) { }

        return Helper.encodeBase64(json,'@');
    }

}
