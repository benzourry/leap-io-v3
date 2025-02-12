package com.benzourry.leap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "leap_dynamic_config")
public class DynamicConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
//    @Column(name = "CONFIG_KEY")
    private String prop;
//    @Column(name = "CONFIG_VALUE")
    private String value;

}