package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class UserMixin {
    @JsonIgnoreProperties({"id","allowReg","needApproval"})
    public interface GroupNameOnly {}

    @JsonIgnoreProperties({"password", "accountNonExpired", "accountNonLocked", "credentialsNonExpired","emailVerified"})
    public interface Attributes {}

    @JsonIgnoreProperties({"providerToken"})
    public interface NoProviderToken {}

}
