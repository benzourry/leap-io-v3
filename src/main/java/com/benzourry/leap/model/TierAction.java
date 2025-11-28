package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.LongListToStringConverter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

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

    @Column(name = "NEXT_TIER")
    Long nextTier; // if action=goTier

    @Column(name = "MAILER")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> mailer; // if action=goTier

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

    public String get_pre(){
        return Helper.encodeBase64(Helper.optimizeJs(this.pre),'@');
    }



}
