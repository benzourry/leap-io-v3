package com.benzourry.leap.utility.jsonresponse;

public final class ResponseWrapperImpl implements ResponseWrapper {

    private final Object originalResponse;
    private final JsonResponse jsonResponse;

    public ResponseWrapperImpl(Object originalResponse, JsonResponse jsonResponse) {
        super();
        this.originalResponse = originalResponse;
        this.jsonResponse = jsonResponse;
    }

    @Override
    public boolean hasJsonMixins() {
        return this.jsonResponse.mixins().length > 0;
    }

    @Override
    public JsonResponse getJsonResponse() {
        return this.jsonResponse;
    }

    @Override
    public Object getOriginalResponse() {
        return this.originalResponse;
    }

}