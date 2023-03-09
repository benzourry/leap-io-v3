package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class AppMixin {
    @JsonIgnoreProperties({"appDomain","layout","tag","price","useGoogle","useUnimas","useFacebook",
            "useGithub","useLinkedin","useEmail","useAzuread","useTwitter","publicAccess","navis","startPage","reg","x"})
    public interface AppBasic{}

    @JsonIgnoreProperties({"access","app"})
    public interface NaviGroupBasic{}

}
