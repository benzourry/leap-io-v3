package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class DashboardMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"charts","app","size","code","type","sortOrder","wide"})
    public interface DashboardBasicList {}

    @JsonIgnoreProperties({"app"})
    public interface DashboardOne {}

    @JsonIgnoreProperties({"app","size","type","sortOrder","code"})
    public interface BasicDashboard {}

    @JsonIgnoreProperties({"rootCode","fieldCode","rootValue","fieldValue","agg","canView", "status","statusFilter","form"})
    public interface BasicChart {}

}
