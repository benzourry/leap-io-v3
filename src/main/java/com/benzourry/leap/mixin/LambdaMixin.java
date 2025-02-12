package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class LambdaMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"data","next","actions","app","form","dataset","sortOrder"})
    public interface LambdaBasicList {}

    @JsonIgnoreProperties({"access","scheduled","freq","clock","dayOfWeek","dayOfMonth","monthOfYear",
    "publicAccess","binds","app","data"})
    public interface LambdaOneInfo {}

}
