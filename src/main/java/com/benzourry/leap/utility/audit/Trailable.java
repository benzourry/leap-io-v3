package com.benzourry.leap.utility.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Trailable {
    @JsonIgnore
    Long getTrailId();

    @JsonIgnore
    String getTrailModule();

    @JsonIgnore
    String getTrailScope();

    @JsonIgnore
    Long getTrailAppId();
}