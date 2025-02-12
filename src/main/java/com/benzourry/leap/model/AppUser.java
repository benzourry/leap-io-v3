package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@Entity
@Table(name="APP_USER")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @JoinColumn(name = "USER", referencedColumnName = "ID")
    @ManyToOne(optional = false)
//    @JsonIgnore
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    User user;

    @JoinColumn(name = "USER_GROUP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    UserGroup group;

    @Column(name = "USER",insertable=false, updatable=false)
    Long userId;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "TAGS", length = 2000)
    String tags;

    @Column(name = "STATUS", length = 50)
    String status; // pending, activated, rejected

//    @Type(value = JsonType.class)
//    @Column(columnDefinition = "json")
//    private JsonNode data;

    public AppUser(){}
    public AppUser(Long id, User user, UserGroup group, String status){
        this.id = id; this.user = user; this.group=group; this.status = status;
    }


    public void setTags(List<String> val){
        if (val!=null){
            this.tags = val.stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

    }

    public List<String> getTags(){
        if (!Helper.isNullOrEmpty(this.tags)) {
            return Arrays.asList(this.tags.split(",")).stream().collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }

}
