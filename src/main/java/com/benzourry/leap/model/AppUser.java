package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;

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

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "STATUS", length = 50)
    String status; // pending, activated, rejected

    public AppUser(){}
    public AppUser(Long id, User user, UserGroup group, String status){
        this.id = id; this.user = user; this.group=group; this.status = status;
    }
}
