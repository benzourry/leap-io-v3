package com.benzourry.leap.model;//package com.benzourry.reka.model;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.Type;
//
//import jakarta.persistence.*;
//import java.io.Serializable;
//import java.util.Set;
//
//@Setter
//@Getter
//@Entity
//@Table(name="ELEMENT")
//public class Element  implements Serializable {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    Long id;
//
//    @Column(name = "LABEL", length = 2000)
//    String label;
//
//    @Column(name = "CODE", length = 2000)
//    String code;
//
//    @Column(name = "TYPE", length = 500)
//    String type;
//
//    @Column(name = "SUB_TYPE", length = 500)
//    String subType;
//
//    @Column(name = "HINT", length = 4000)
//    String hint;
//
//    @Column(name = "PRE", length = 2000)
//    String pre;
//
//    @Column(name = "POST", length = 2000)
//    String post;
//
//    @Column(name = "PLACEHOLDER", length = 4000)
//    String placeholder;
//
//    @Column(name = "F", length = 4000)
//    String f;
//
//    @Column(name = "SIZE")
//    String size;
//
//    @Column(name = "DATASOURCE")
//    Long dataSource;
//
//    @Type(type = "json")
//    @Column(columnDefinition = "json")
//    JsonNode dataSourceInit;
//
//    @Column(name = "OPTIONS", length = 4000)
//    String options;
//
////    @Column(name = "SORT_ORDER")
////    Long sortOrder;
//
//    @Column(name = "HIDE_LABEL")
//    boolean hideLabel;
//
//    @Column(name = "HIDDEN")
//    boolean hidden;
//
//    @Column(name = "READ_ONLY")
//    boolean readOnly;
//
//
//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "element", orphanRemoval = true, fetch = FetchType.LAZY)
//    @JsonManagedReference("element-item")
//    @OrderBy("sortOrder ASC")
//    Set<Element> items;
//
//
//    @JoinColumn(name = "PARENT", referencedColumnName = "ID")
//    @ManyToOne
//    @JsonBackReference("element-item")
//    Element element;
//
//
//    @Column(name = "SORT_ORDER")
//    Long sortOrder;
//
//
//    @JoinColumn(name = "FORM", referencedColumnName = "ID")
//    @ManyToOne
//    @JsonBackReference("form-elements")
//    Form form;
//
////    @ElementCollection(fetch = FetchType.LAZY)
////    @CollectionTable(
////            name="FORM_ITEM_V",
////            joinColumns=@JoinColumn(name="FORM_ITEM_ID")
////    )
////    @Column(name="V", length = 4000)
////    @MapKeyColumn(name="K")
////    private Map<String, String> v = new HashMap<String, String>();
//
//    @Type(type = "json")
//    @Column(columnDefinition = "json")
//    private JsonNode v;
//
//}
