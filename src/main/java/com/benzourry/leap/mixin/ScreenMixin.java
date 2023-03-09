package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class ScreenMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"data","next","actions","app","form","dataset","sortOrder"})
    public interface ScreenBasicList {}

    @JsonIgnoreProperties({"app"})
    public interface ScreenOne {}

    @JsonIgnoreProperties({"app","canBlast","canDelete","canEdit","canReset","canRetract","canView","statusFilter"})
    public interface ScreenOneDataset {}

}
