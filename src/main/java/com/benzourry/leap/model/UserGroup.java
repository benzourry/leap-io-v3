package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

//    @Column(name = "USERS", length = 8000)
//    String users;


    @Column(name = "ALLOW_REG")
    boolean allowReg;

    @Column(name = "NEED_APPROVAL")
    boolean needApproval;

//    @Column(name = "DATA_ENABLED")
//    boolean dataEnabled;
//
//    @Column(name = "DATA_FIELDS")
//    String dataFields;

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
    String accessList;



    public void setAccessList(List<Long> val){
        if (!Helper.isNullOrEmpty(val)) {
            this.accessList = val.stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
    }

    public List<Long> getAccessList(){
        if (!Helper.isNullOrEmpty(this.accessList)) {
            return Arrays.asList(this.accessList.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }


}
