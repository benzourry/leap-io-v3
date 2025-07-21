package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class EntryMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"approver","form","createdBy","modifiedBy"})
    public interface EntryList{}

    @JsonIgnoreProperties({"appId","emailVerified","firstLogin","lastLogin","once","provider","providerId","status","providerToken"})
    public interface EntryListApprovalApprover{}

    @JsonIgnoreProperties({"tier"})
    public interface EntryListApproval{}

    @JsonIgnoreProperties({"orgMap","orgMapPointer","orgMapParam",
            "canApprove","submitMailer","submitCb","assignMailer",
            "assignCb","resubmitMailer","resubmitCallback","approveMailer",
            "approveCb","canReject","rejectMailer","rejectCb",
            "canReturn","returnMailer","returnCb","canSkip",
            "canRemark","alwaysApprove"})
    public interface EntryListApprovalTier{}

    @JsonIgnoreProperties({"size","align","style","code","type","description","sortOrder","main","hideHeader","pre","enabledFor","parent","items"})
    public interface EntryListApprovalTierSection{}

    @JsonIgnoreProperties({"form"})
    public interface NoForm{}

//    @JsonIgnoreProperties({"lat","lng","$id"})
//    public interface JsonNodeF{}

//    @JsonIgnoreProperties({"creator","subject","content"})
//    public interface MailerList{}

//    @JsonIgnoreProperties({"lookup","ordering"})
//    public interface MailerEntryList{}

//    @JsonIgnoreProperties({"canDelete","canEdit","canView","canRetract","ui","uiTemplate","items","showAction","showStatus"})
//    public interface NoDatasetItem {
//    }
}
