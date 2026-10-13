package com.benzourry.leap.utility.audit;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MetaTrailEvent {
    private Long moduleId;
    private String module;
    private String scope;
    private String action;
    private Long appId;
    private JsonNode data;
    private String email;
}