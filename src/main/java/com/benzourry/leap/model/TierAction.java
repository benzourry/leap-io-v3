package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@Entity
@Table(name="TIER_ACTION")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TierAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "LABEL")
    String label;

    @Column(name = "CODE")
    String code;

    @Column(name = "ACTION")
    String action; //nextTier, prevTier, goTier

    @Column(name = "ICON")
    String icon; //nextTier, prevTier, goTier

    @Column(name = "COLOR")
    String color; //nextTier, prevTier, goTier

//    @Column(name = "NEXT_STATUS")
//    String nextStatus; //nextTier, prevTier, goTier

    @Column(name = "NEXT_TIER")
    Long nextTier; // if action=goTier

    @Column(name = "MAILER")
    String mailer; // if action=goTier

    @Column(name = "USER_EDIT")
    boolean userEdit;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "PRE", length = 2000)
    String pre;

    @JoinColumn(name = "TIER", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("tier-actions")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Tier tier;

    public void setMailer(List<Long> val){
        this.mailer = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getMailer(){
        if (!Helper.isNullOrEmpty(this.mailer)) {
            return Arrays.stream(this.mailer.split(","))
                    .map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }


}
