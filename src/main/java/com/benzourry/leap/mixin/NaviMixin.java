package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class NaviMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"sections","app","items","dashboards","datasets","tiers","rdEndpoint","rdQualifier","rdRoot","rlEndpoint","rlQualifier","rlRoot","idField","elements","tabs",
            "category","nav","align","f","prev","codeFormat","onSave","onSubmit","hideStatus","validateSave","canSave",
            "canEdit","canRetract","canSubmit","counter","addMailer","updateMailer","type","icon",
            "retractMailer","access"})
    public interface FormList{}

//    @JsonIgnoreProperties({"sections","app","items","tiers","tabs","rdEndpoint","rdQualifier","rdRoot","rlEndpoint","rlQualifier","rlRoot","idField"})
//    public interface AppFormListWithDetail{}

    @JsonIgnoreProperties({"ui","uiTemplate","status","sortOrder","items","filters","presetFilters","form","app","canView",
            "canEdit","canRetract","canDelete","showStatus","showAction","exportXls",
            "exportCsv","exportPdf","exportPdfLayout","size","code","next","statusFilter","canReset","canBlast","blastTo","showIndex","wide","screen","defaultSort","access"})
    public interface DatasetList {
    }

    @JsonIgnoreProperties({"form","app","charts","size","type","code","wide","access"})
    public interface DashboardList {
    }

    @JsonIgnoreProperties({"form","app","dataset","sortOrder","data","next","access"})
    public interface ScreenList {
    }

    @JsonIgnoreProperties({"id","label"})
    public interface ScreenActionList {
    }

    @JsonIgnoreProperties({"app","jsonRoot","extraProp","descProp","codeProp","url","headers","responseType","email","dataEnabled","shared","sourceType","access"})
    public interface LookupList {
    }

    @JsonIgnoreProperties({"sections","app","items","dashboards","datasets","tiers","rdEndpoint","rdQualifier","rdRoot","rlEndpoint","rlQualifier","rlRoot","idField","elements","tabs",
            "category","nav","align","f","prev","codeFormat","onSave","onSubmit","hideStatus","validateSave","canSave",
            "canEdit","canRetract","canSubmit","counter","addMailer","updateMailer","type","icon",
            "retractMailer"})
    public interface FormListAccess{}

//    @JsonIgnoreProperties({"sections","app","items","tiers","tabs","rdEndpoint","rdQualifier","rdRoot","rlEndpoint","rlQualifier","rlRoot","idField"})
//    public interface AppFormListWithDetail{}

    @JsonIgnoreProperties({"ui","uiTemplate","status","sortOrder","items","filters","presetFilters","form","app","canView",
            "canEdit","canRetract","canDelete","showStatus","showAction","exportXls",
            "exportCsv","exportPdf","exportPdfLayout","size","code","next","statusFilter","canReset","canBlast","blastTo","showIndex","wide","screen","defaultSort"})
    public interface DatasetListAccess {
    }

    @JsonIgnoreProperties({"form","app","charts","size","type","code","wide"})
    public interface DashboardListAccess {
    }

    @JsonIgnoreProperties({"form","app","dataset","sortOrder","data","next"})
    public interface ScreenListAccess {
    }

    @JsonIgnoreProperties({"app","jsonRoot","extraProp","descProp","codeProp","url","headers","responseType","email","dataEnabled","shared","sourceType"})
    public interface LookupListAccess {
    }
}
