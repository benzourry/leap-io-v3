package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class ScreenMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"data","_data","next","actions","app","form","dataset","sortOrder"})
    public interface ScreenBasicList {}

    @JsonIgnoreProperties({"app","_data"})
    public interface ScreenOne {}
    @JsonIgnoreProperties({"app","data"})
    public interface ScreenOneRun {}
    @JsonIgnoreProperties({"f"})
    public interface ScreenActionOneRun {}

    @JsonIgnoreProperties({"app","canBlast","canDelete","canEdit","canReset","canRetract","canView","statusFilter"})
    public interface ScreenOneDataset {}

}
