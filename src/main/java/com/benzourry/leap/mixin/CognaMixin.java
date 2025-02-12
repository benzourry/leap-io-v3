package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class CognaMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"data","next","actions","app","form","dataset","sortOrder","embedModelType","embedModelName","augmentor",
            "embedModelApiKey","embedMaxResult","maxChatMemory","chunkLength","chunkOverlap",
    "inferModelType","inferModelName","inferModelApiKey","vectorStoreType","temperature","systemMessage","postMessage","email",
    "data","sources","app"})
    public interface CognaBasicList {}

    @JsonIgnoreProperties({"access","scheduled","freq","clock","dayOfWeek","dayOfMonth","monthOfYear",
    "publicAccess","binds","app","data"})
    public interface CognaOneInfo {}


    @JsonIgnoreProperties({"access","scheduled","freq","clock","dayOfWeek","dayOfMonth","monthOfYear",
    "publicAccess","app","data","embedModelType","embedModelName", "augmentor",
            "embedModelApiKey","embedMaxResult","maxChatMemory","chunkLength","chunkOverlap",
            "inferModelType","inferModelName","inferModelApiKey","vectorStoreType","temperature","systemMessage","postMessage","email",
            "data","sources"})
    public interface CognaHideSensitive {}

}
