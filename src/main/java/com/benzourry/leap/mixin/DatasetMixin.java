package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class DatasetMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"ui","uiTemplate","status",
            "showStatus","showAction","exportXls",
            "exportCsv","exportPdf","exportPdfLayout","items","filters","presetFilters",
            "app","size","code","statusFilter","sortOrder","wide","showIndex","canBlast","blastTo","defaultSort",
            "inpop","canPrint","showAction","wide","defSortDir",
            "defSortField","x","actions"
    })
    public interface DatasetBasicList {}


    @JsonIgnoreProperties({"ui","uiTemplate","app"})
    public interface DatasetOne {}

    @JsonIgnoreProperties({"ui","uiTemplate","app"})
    public interface DatasetOneRun {}

    @JsonIgnoreProperties({"app","canEdit","canRetract","canSave","canSubmit","codeFormat","f","counter","tabs","sections","validateSave",
            "f","onSave","onSubmit","onView","_f","_onSave","_onSubmit","_onView"})
    public interface DatasetOneForm {}
    @JsonIgnoreProperties({"app","canEdit","canRetract","canSave","canSubmit","codeFormat","f","counter","tabs","sections","validateSave",
            "f","onSave","onSubmit","onView","_f","_onSave","_onSubmit","_onView"})
    public interface DatasetOneFormRun {}

    @JsonIgnoreProperties({"pre"})
    public interface DatasetItemOneRun {}

    @JsonIgnoreProperties({"_f","_pre"})
    public interface DatasetActionOne {}
    @JsonIgnoreProperties({"f","pre"})
    public interface DatasetActionOneRun {}

}
