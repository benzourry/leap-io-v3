package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Date;

@Setter
@Getter
@Entity
@Table(name="COGNA_SOURCE")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CognaSource extends Schedulable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "TYPE", length = 25)
    String type; //[dataset, entry, user]

    @Column(name = "SRC_ID")
    Long srcId; //10025 -- id of the component

    @Column(name = "APP_ID")
    Long appId; //10025 -- id of the component

    @Column(name = "SRC_URL")
    String srcUrl; //https://www.unimas.my/policy.pdf

    @Column(name = "PARAMS", length = 250)
    String params;

    @Column(name = "SENTENCE_TPL", length = 2000)
    String sentenceTpl;

    @Column(name = "CATEGORY_TPL", length = 2000)
    String categoryTpl;

    @Column(name = "LAST_INGEST")
    Date lastIngest;

//    @Column(name = "SCHEDULED")
//    Boolean scheduled;
//
//    @Column(name = "FREQ")
//    String freq; // everyday, everyweek, everymonth;
//
//    @Column(name = "CLOCK")
//    String clock; //everyday:0101
//
//    @Column(name = "DAY_OF_WEEK")
//    Integer dayOfWeek; //everyweek:1,2,3,4,5,6,7
//
//    @Column(name = "DAY_OF_MONTH")
//    Integer dayOfMonth; // 25
//
//    @Column(name = "MONTH_OF_YEAR")
//    Integer monthOfYear; // 2

    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("cogna-source")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Cogna cogna;

}
