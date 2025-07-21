package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class MetadataMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"app","live"})
    public interface Form {}

    @JsonIgnoreProperties({"app"})
    public interface Dataset {}

    @JsonIgnoreProperties({"app",})
    public interface Dashboard {}

    @JsonIgnoreProperties({"dashboard"})
    public interface Chart{}

    @JsonIgnoreProperties({"app"})
    public interface Screen {
    }

    @JsonIgnoreProperties({"app"})
    public interface Lookup {
    }
    @JsonIgnoreProperties({"app"})
    public interface Role {
    }
    @JsonIgnoreProperties({"app"})
    public interface Bucket {
    }
    @JsonIgnoreProperties({"app"})
    public interface Endpoint {
    }
    @JsonIgnoreProperties({"app"})
    public interface Email {
    }

    @JsonIgnoreProperties({"app"})
    public interface Cogna {
    }

    @JsonIgnoreProperties({"app"})
    public interface Lambda {
    }


}
