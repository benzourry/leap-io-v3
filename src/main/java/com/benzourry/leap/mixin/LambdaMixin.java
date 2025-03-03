package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class LambdaMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"scheduled","freq","clock","dayOfWeek","dayOfMonth","monthOfYear",
            "publicAccess","binds","app","data"})
    public interface LambdaBasicList {}

    @JsonIgnoreProperties({"scheduled","freq","clock","dayOfWeek","dayOfMonth","monthOfYear",
    "publicAccess","binds","app","data"})
    public interface LambdaOneInfo {}

}
