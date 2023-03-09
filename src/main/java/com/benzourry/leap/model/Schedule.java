package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="SCHEDULE")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schedule implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

//    @Size(max = 512)
//    @NotNull
//    @Column(name = "CONFIG_VALUE")
//    String configValue;

    String freq; // everyday, everyweek, everymonth;

    String clock; //everyday:0101

    @Column(name = "DAY_OF_WEEK")
    Integer dayOfWeek; //everyweek:1,2,3,4,5,6,7

    @Column(name = "DAY_OF_MONTH")
    Integer dayOfMonth; // 25

    @Column(name = "MONTH_OF_YEAR")
    Integer monthOfYear; // 2

    String type; //mailblast

    @Column(name = "DATASET_ID")
    Long datasetId;

    @Column(name = "MAILER_ID")
    Long mailerId;

    @Column(name = "EMAIL")
    String email;


    @Column(name = "ENABLED")
    private Integer enabled;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

}
