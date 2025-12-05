package com.benzourry.leap.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by MohdRazif on 6/24/2015.
 */
public class LambdaMixin {
//    public interface Book extends BasicMixin.AuditableEntity{}

    @JsonIgnoreProperties({"scheduled","freq","clock","dayOfWeek","dayOfMonth","monthOfYear",
            "publicAccess","binds","app","data","signa"})
    public interface LambdaBasicList {}

    @JsonIgnoreProperties({"scheduled","freq","clock","dayOfWeek","dayOfMonth","monthOfYear",
    "publicAccess","binds","app","data"})
    public interface LambdaOneInfo {}

    @JsonIgnoreProperties({"imagePath","reason","keyPath","hashAlg","keyAlg","keystoreType",
    "password","ecCurve","stampLlx","stampLly","stampUrx","stampUry","showStamp"})
    public interface SignaBasicList {}

    @JsonIgnoreProperties({"contractAddress","privateKey","contract","app"})
    public interface KryptaWalletBasicList {}

    @JsonIgnoreProperties({"sol","abi","bin","abiSummary"})
    public interface KryptaContractBasicList {}

}
