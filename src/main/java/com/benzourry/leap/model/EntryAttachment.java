package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Setter
@Getter
@Table(name = "ENTRY_ATTACHMENT", indexes = {
    @Index(name = "idx_entry_attachment_item_id", columnList = "ITEM_ID"),
    @Index(name = "idx_entry_attachment_entry_id", columnList = "ENTRY_ID"),
    @Index(name = "idx_entry_attachment_bucket_id", columnList = "BUCKET_ID"),
})
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
