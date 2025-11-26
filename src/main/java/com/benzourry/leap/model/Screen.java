package com.benzourry.leap.model;


import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.*;
import java.util.stream.Collectors;

@Setter
@Getter
@Entity
@Table(name="SCREEN")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Screen extends BaseEntity implements Serializable{


    // Reuse a single ObjectMapper instance
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

    @Column(name = "TYPE")
    String type; // [qr,search,view,form,list,]

//    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
//    @ManyToOne
//    @NotFound(action = NotFoundAction.IGNORE)
//    @OnDelete(action = OnDeleteAction.NO_ACTION)
//    UserGroup access;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode data;

    @JsonIgnore
    @Column(name = "DATA",insertable=false, updatable=false)
    String dataText;

    @Column(name = "NEXT")
    Long next;

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "screen", orphanRemoval = true, fetch = FetchType.LAZY)
//    @JsonManagedReference("screen-elements")
//    private Set<Element> elements = new HashSet<>();
//
    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "SHOW_ACTION")
    boolean showAction;


    @Column(name = "CAN_PRINT")
    boolean canPrint;

    @Column(name = "WIDE")
    boolean wide;

    //
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
    String accessList;

    public String get_data(){

        if (this.getData()==null) return null;

//        long start = System.currentTimeMillis();

        String json = this.dataText;
//        for (int i=0;i<2000;i++) {

        Map<String, Object> data = MAPPER.convertValue(this.getData(), HashMap.class);

        data.put("f", Helper.optimizeJs(this.getData().at("/f").asText()));
        data.put("content", Helper.optimizeHtml(this.getData().at("/content").asText()));
        data.put("pretext", Helper.optimizeHtml(this.getData().at("/pretext").asText()));
        data.put("posttext", Helper.optimizeHtml(this.getData().at("/posttext").asText()));

        try {
            json = MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
        }
//        }
//        long end = System.currentTimeMillis();
//        System.out.println("duration:"+ (end-start));

        return Helper.encodeBase64(json,'@');
    }


//    public String get_data(){
//
//        if (this.getData()==null) return null;
//
//        long start = System.currentTimeMillis();
//
//        String json = this.dataText;
//
//        for (int i=0;i<2000;i++) {
//            JsonNode rootCopy = this.getData().deepCopy();
//            ObjectNode data = (ObjectNode) rootCopy;
//
//            data.put("f", Helper.optimizeJs(this.getData().at("/f").asText()));
//            data.put("content", Helper.optimizeHtml(this.getData().at("/content").asText()));
//            data.put("pretext", Helper.optimizeHtml(this.getData().at("/pretext").asText()));
//            data.put("posttext", Helper.optimizeHtml(this.getData().at("/posttext").asText()));
//
//            try {
//                json = MAPPER.writeValueAsString(data);
//            } catch (JsonProcessingException e) {
////            throw new RuntimeException(e);
//            }
//        }
//
//        long end = System.currentTimeMillis();
//        System.out.println("duration:"+ (end-start));
//
////        return Helper.encodeBase64(this.dataText);
//        return Helper.encodeBase64(json,'@');
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
