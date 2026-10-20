package com.benzourry.leap.model;

import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Setter
@Getter
@Entity
@Table(name="LAMBDA_BIND")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LambdaBind implements Trailable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "TYPE", length = 25)
    String type; //[dataset, entry, user]

    @Column(name = "SRC_ID")
    Long srcId; //10025 -- id of the component

    @Column(name = "PARAMS", length = 250)
    String params;

    @JoinColumn(name = "LAMBDA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("lambda-bind")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Lambda lambda;

    @Column(name = "LAMBDA",insertable=false, updatable=false)
    Long lambdaId;

    @Override
    public Long getTrailId() {
        return this.id;
    }

    @Override
    public String getTrailModule() {
        return "lambda_bind";
    }

    @Override
    public String getTrailScope() {
        Long safeLambdaId = this.lambdaId != null ? this.lambdaId : (this.lambda != null ? this.lambda.getId() : null);
        return "lambda_"+ safeLambdaId + "_bind_" + this.id;
    }

    @Override
    public Long getTrailAppId() {
        // If form is a corrupted proxy, this safely avoids triggering the EntityNotFoundException
        if (this.lambda == null) return null;
        try {
            return this.lambda.getAppId();
        } catch (Exception e) {
            return null; // Fallback if the parent App is missing
        }
    }

}
