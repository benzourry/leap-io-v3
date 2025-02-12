package com.benzourry.leap.model;//package com.benzourry.leap.model;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.vladmihalcea.hibernate.type.json.JsonType;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.Type;
//
//import jakarta.persistence.*;
//import java.io.Serializable;
//
//@Setter
//@Getter
//@Entity
//@Table(name="SUBSCRIPTION")
//@JsonInclude(JsonInclude.Include.NON_NULL)
//public class Subscription extends BaseEntity implements Serializable {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    Long id;
//
//    @Type(value = JsonType.class)
//    @Column(columnDefinition = "json")
//    private JsonNode sub;
//
//    @Column(name = "EMAIL")
//    String email;
//}
