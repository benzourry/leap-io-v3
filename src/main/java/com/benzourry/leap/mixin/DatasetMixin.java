package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class DatasetMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"ui","uiTemplate","next","screen","status",
            "adminOnly","canEdit","canView","canRetract","canDelete","showStatus","showAction","exportXls",
            "exportCsv","exportPdf","exportPdfLayout","items","filters","presetFilters",
            "app","size","code","statusFilter","sortOrder","form","canReset","wide","showIndex","canBlast","blastTo","defaultSort",
            "inpop","canApprove","canPrint","showAction","wide","defSortDir",
            "defSortField","x","facet"
    })
    public interface DatasetBasicList {}


    @JsonIgnoreProperties({"ui","uiTemplate","app"})
    public interface DatasetOne {}

    @JsonIgnoreProperties({"app","canEdit","canRetract","canSave","canSubmit","codeFormat","f","counter","tabs","sections","validateSave"})
    public interface DatasetOneForm {}

}
