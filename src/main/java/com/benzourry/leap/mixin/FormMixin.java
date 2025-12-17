package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class FormMixin {

    @JsonIgnoreProperties({"sections","app","items","tiers","rdEndpoint","rdQualifier","rdRoot","rlEndpoint",
            "rlQualifier","rlRoot","idField","elements","tabs","category","prev","icon","nav",
            "codeFormat","admin","access","f","hideForm","startDate","endDate","inactive",
            "publicAccess","counter","align","canEdit","canRetract","canSave","canSubmit",
            "hideStatus","validateSave","updateMailer","addMailer","retractMailer","singleQ",
            "onSave","onSubmit","onView",
            "_f","_onSave","_onSubmit","_onView"})
    public interface FormBasicList {}

    @JsonIgnoreProperties({"app","_f","_onSave","_onSubmit","_onView"})
    public interface FormOne {}

    @JsonIgnoreProperties({"app","f","onSave","onSubmit","onView",
            "updateMailer","addMailer","retractMailer"})
    public interface FormOneRun {}

    @JsonIgnoreProperties({"pre","post","f","placeholder"})
    public interface FormItemOneRun {}
    @JsonIgnoreProperties({"_pre","_post","_f","_placeholder"})
    public interface FormItemOne {}

    @JsonIgnoreProperties({"pre"})
    public interface FormSectionOneRun {}

    @JsonIgnoreProperties({"pre"})
    public interface FormTabOneRun {}

    @JsonIgnoreProperties({"pre","post"})
    public interface FormEntityHidePrePostOneRun {}

    @JsonIgnoreProperties({"sections","app","items","tiers","rdEndpoint","rdQualifier","rdRoot",
            "rlEndpoint","rlQualifier","rlRoot","idField","elements","tabs","category",
            "prev","icon","nav","codeFormat","_f","_onSave","_onSubmit","_onView"})
    public interface FormList {}

    @JsonIgnoreProperties({"sections","app","items","tiers","tabs","rdEndpoint","rdQualifier",
            "rdRoot","rlEndpoint","rlQualifier","rlRoot","idField"})
    public interface AppFormListWithDetail{}

    @JsonIgnoreProperties({"canDelete","canEdit","canView","canRetract","ui","uiTemplate","items","showAction","showStatus"})
    public interface NoDatasetItem {}

    @JsonIgnoreProperties({"charts","size","sortOrder","type"})
    public interface NoDashboardChart {}

}
