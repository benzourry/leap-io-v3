package com.benzourry.leap.model;

import com.benzourry.leap.utility.LongListToStringConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name="USER_GROUP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME", length = 250)
    String name;

    @Column(name = "DESCRIPTION",length = 2000)
    String description;

    @Column(name = "ALLOW_REG")
    boolean allowReg;

    @Column(name = "NEED_APPROVAL")
    boolean needApproval;

    @Column(name = "TAG_ENABLED")
    boolean tagEnabled;

    @Column(name = "TAG_DS")
    Long tagDs;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

    @Column(name = "ACCESS_LIST")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> accessList;

}
