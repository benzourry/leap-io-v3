package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class LookupMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"app"})
    public interface LookupOne {}

    @JsonIgnoreProperties({"codeProp","descProp","url","extraProp","headers","jsonRoot","responseType",
            "email","app","dataEnabled","method","endpoint","dataFields"})
    public interface LookupBasicList {}

    @JsonIgnoreProperties({"lookup","enabled","ordering"})
    public interface LookupEntryList{}

    @JsonIgnoreProperties({"lookup"})
    public interface LookupEntryListFull{}

//    @JsonIgnoreProperties({"canDelete","canEdit","canView","canRetract","ui","uiTemplate","items","showAction","showStatus"})
//    public interface NoDatasetItem {
//    }
}
