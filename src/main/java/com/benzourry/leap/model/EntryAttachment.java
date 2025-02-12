package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Setter
@Getter
@Table(name = "ENTRY_ATTACHMENT")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntryAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long fileSize;

    String fileName;

    String fileType;

    String fileUrl;

    String email;

    // Upload message/success
    String message;
    boolean success;

    String sMessage; // Threat Found! / File OK
    String sStatus; // OK, FAILED,

    @Temporal(TemporalType.TIMESTAMP)
    Date timestamp;

    Long itemId;

    Long entryId;

    Long appId;

    @Column(name = "ITEM_LABEL", length = 250)
    String itemLabel;

    @Column(name = "BUCKET_CODE")
    String bucketCode;

    @Column(name = "BUCKET_ID")
    Long bucketId;



}
