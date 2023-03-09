package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class EndpointMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"auth","app","method","url"})
    public interface EndpointBasicList {}

//    @JsonIgnoreProperties({"creator","subject","content"})
//    public interface MailerList{}

//    @JsonIgnoreProperties({"lookup","ordering"})
//    public interface MailerEntryList{}

//    @JsonIgnoreProperties({"canDelete","canEdit","canView","canRetract","ui","uiTemplate","items","showAction","showStatus"})
//    public interface NoDatasetItem {
//    }
}
