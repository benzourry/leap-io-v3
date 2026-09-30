package com.benzourry.leap.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by MohdRazif on 12/7/2016.
 */

@Service("liveService")
public class LiveService {

    private SimpMessagingTemplate template;

    public LiveService(
                       SimpMessagingTemplate template){
        this.template = template;
    }

    public record LiveMessage(
            Long id,
            String sender,
            String senderName,
            String channel,
            String content,
            String action,
            Date timestamp
    ){}

    public LiveMessage pass(String channel, LiveMessage message){

        template.convertAndSend("/"+channel, message.content());

        return message;
    }

}
