package com.benzourry.leap.model;

import lombok.Data;

import java.util.Date;

@Data
public class PushMessage {
    private String forEmail;
    private String notificationFor;
    private String message;
    private Date timestamp;
}
