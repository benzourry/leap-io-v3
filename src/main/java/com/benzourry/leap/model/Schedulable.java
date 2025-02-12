package com.benzourry.leap.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Setter
@Getter
public class Schedulable {

    @Column(name = "SCHEDULED")
    Boolean scheduled;

    @Column(name = "FREQ")
    String freq; // everyday, everyweek, everymonth;

    @Column(name = "CLOCK")
    String clock; //everyday:0101

    @Column(name = "DAY_OF_WEEK")
    Integer dayOfWeek; //everyweek:1,2,3,4,5,6,7

    @Column(name = "DAY_OF_MONTH")
    Integer dayOfMonth; // 25

    @Column(name = "MONTH_OF_YEAR")
    Integer monthOfYear; // 2

}
