package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Collections;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name="APP_USER", indexes = {
        @Index(name = "idx_app_user_user", columnList = "USER"),
        @Index(name = "idx_app_user_user_group", columnList = "USER_GROUP")
})
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

    // Cached parsed tags
    @Transient
    private List<String> tagList;


    public AppUser(){}
    public AppUser(Long id, User user, UserGroup group, String status){
        this.id = id; this.user = user; this.group=group; this.status = status;
    }

    public void setTags(List<String> val){
        if (val != null) {
            this.tags = String.join(",", val);   // much faster & allocates less
            this.tagList = val;                  // optional: cache directly
        } else {
            this.tags = null;
            this.tagList = null;
        }
    }

    public List<String> getTags() {
        if (tagList != null) {
            return tagList;
        }
        if (Helper.isNullOrEmpty(this.tags)) {
            return Collections.emptyList();      // no allocation
        }

        // Fast split without regex:
        this.tagList = List.of(this.tags.split(",", -1));
        return this.tagList;
    }

}
