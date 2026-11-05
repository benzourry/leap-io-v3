package com.benzourry.leap.utility;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WsContext {
    private static SimpMessagingTemplate template;

    public WsContext(SimpMessagingTemplate template) {
        WsContext.template = template;
    }

    public static SimpMessagingTemplate getTemplate() {
        return template;
    }
}