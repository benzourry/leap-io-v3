package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class FormMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"sections","app","items","tiers","rdEndpoint","rdQualifier","rdRoot","rlEndpoint","rlQualifier","rlRoot","idField","elements","tabs","category","prev","icon","nav","codeFormat","admin","access","f","hideForm","startDate","endDate","inactive","publicAccess","counter","align","canEdit","canRetract","canSave","canSubmit","hideStatus","validateSave","updateMailer","addMailer","retractMailer","singleQ","onSave","onSubmit","onView"})
    public interface FormBasicList {}


    @JsonIgnoreProperties({"app"})
    public interface FormOne {}

    @JsonIgnoreProperties({"sections","app","items","tiers","rdEndpoint","rdQualifier","rdRoot","rlEndpoint","rlQualifier","rlRoot","idField","elements","tabs","category","prev","icon","nav","codeFormat",})
    public interface FormList {}

    @JsonIgnoreProperties({"sections","app","items","tiers","tabs","rdEndpoint","rdQualifier","rdRoot","rlEndpoint","rlQualifier","rlRoot","idField"})
    public interface AppFormListWithDetail{}

    @JsonIgnoreProperties({"canDelete","canEdit","canView","canRetract","ui","uiTemplate","items","showAction","showStatus"})
    public interface NoDatasetItem {
    }

    @JsonIgnoreProperties({"charts","size","sortOrder","type"})
    public interface NoDashboardChart {
    }
}
