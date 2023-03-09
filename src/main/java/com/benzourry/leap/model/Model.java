//package com.benzourry.reka.model;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import lombok.Getter;
//import lombok.Setter;
//
//import jakarta.persistence.*;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//
//@Setter
//@Getter
//@Entity
//@Table(name="MODEL")
//public class Model {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    Long id;
//
//    @Column(name = "NAME")
//    String name;
//
//    @Column(name = "TYPE")
//    String type; //loookup, model, string, date, number
//
//    @Column(name = "CODE")
//    String code;
//
//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "model", orphanRemoval = true, fetch = FetchType.LAZY)
//    @MapKeyColumn(name = "code")
//    @JsonManagedReference("model-items")
//    private Map<String, Model> items = new HashMap<>();
//
//
//    @JoinColumn(name = "PARENT", referencedColumnName = "ID")
//    @ManyToOne
//    @JsonBackReference("model-items")
//    Model model;
//
//
////    @JoinColumn(name = "FORM", referencedColumnName = "ID")
////    @ManyToOne
////    @JsonBackReference("form-models")
////    Form form;
//
////    @OneToMany(cascade = CascadeType.ALL, mappedBy = "model", orphanRemoval = true, fetch = FetchType.LAZY)
////    @JsonManagedReference("model-attribute")
////    @OrderBy("sortOrder ASC")
////    Set<DatasetItem> attributes;
//}
