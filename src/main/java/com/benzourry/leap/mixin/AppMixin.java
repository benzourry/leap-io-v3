package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class AppMixin {
    @JsonIgnoreProperties({"appDomain","layout","tag","price","useGoogle","useUnimas","useUnimasid","useIcatsid","useFacebook",
            "useGithub","useLinkedin","useAnon","useMyDID","useSarawakid","useSsone","useEmail","useAzuread","useTwitter","publicAccess","navis","startPage","reg","x","_x"})
    public interface AppBasic{}

    @JsonIgnoreProperties({"access","app"})
    public interface NaviGroupBasic{}

    @JsonIgnoreProperties({"f","x", "group","clone","status"})
    public interface AppOneRun {}

    @JsonIgnoreProperties({"_f","_x"})
    public interface AppOneDesign {}



}
