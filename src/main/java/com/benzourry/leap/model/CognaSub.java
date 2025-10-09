package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Setter
@Getter
@Entity
@Table(name="COGNA_SUB")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CognaSub {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION")
    String description;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "SUB_ID")
    Long subId;

    @Column(name = "ENABLED")
    boolean enabled;

    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("cogna-sub")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Cogna cogna;

}
