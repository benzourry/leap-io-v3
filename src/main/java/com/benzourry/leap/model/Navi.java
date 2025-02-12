package com.benzourry.leap.model;//package com.benzourry.leap.model;
//
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import lombok.Getter;
//import lombok.Setter;
//
//import jakarta.persistence.*;
//import java.util.ArrayList;
//import java.util.List;
//
//@Setter
//@Getter
//@Entity
//@Table(name="NAVI")
//public class Navi {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    Long id;
//
//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "navi", orphanRemoval = true, fetch = FetchType.LAZY)
//    @JsonManagedReference("navi-group")
//    @OrderBy("sortOrder ASC")
//    List<NaviGroup> groups = new ArrayList<>();
//}
