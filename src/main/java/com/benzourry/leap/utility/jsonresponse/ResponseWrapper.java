package com.benzourry.leap.utility.jsonresponse;

public interface ResponseWrapper {

    boolean hasJsonMixins();

    JsonResponse getJsonResponse();

    Object getOriginalResponse();

}