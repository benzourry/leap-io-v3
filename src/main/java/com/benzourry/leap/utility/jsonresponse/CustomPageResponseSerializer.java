package com.benzourry.leap.utility.jsonresponse;//package com.benzourry.leap.utility.jsonresponse;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.JsonSerializer;
//import com.fasterxml.jackson.databind.MapperFeature;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.web.PagedModel;
//import org.springframework.data.web.PagedResourcesAssembler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Optional;
//
//@Component
//public class CustomPageResponseSerializer extends JsonSerializer<PageImpl> {
//
////    @Autowired
////    private PagedResourcesAssembler<Page> pagedResourcesAssembler;
////    @Autowired
////    private ObjectMapper objectMapper;
//
//    @Override
//    public void serialize(PageImpl page, JsonGenerator jsonGen, SerializerProvider provider) throws IOException {
////        PagedModel pageModel = pagedResourcesAssembler.toModel(page);
//
//        ObjectMapper om = new ObjectMapper()
//                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
//                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
//        jsonGen.writeStartObject();
//        jsonGen.writeFieldName("size0");
//        jsonGen.writeNumber(page.getSize());
//        jsonGen.writeFieldName("number0");
//        jsonGen.writeNumber(page.getNumber());
//        jsonGen.writeFieldName("totalElements0");
//        jsonGen.writeNumber(page.getTotalElements());
//        jsonGen.writeFieldName("last0");
//        jsonGen.writeBoolean(page.isLast());
//        jsonGen.writeFieldName("totalPages0");
//        jsonGen.writeNumber(page.getTotalPages());
//        jsonGen.writeObjectField("sort", page.getSort());
//        jsonGen.writeFieldName("first0");
//        jsonGen.writeBoolean(page.isFirst());
//        jsonGen.writeFieldName("numberOfElements");
//        jsonGen.writeNumber(page.getNumberOfElements());
//        jsonGen.writeFieldName("content");
//        jsonGen.writeRawValue(om.writerWithView(provider.getActiveView()).writeValueAsString(page.getContent()));
//        jsonGen.writeEndObject();
//
////        new ObjectMapper().writeValue(gen, pageResponse);
//    }
//
//    @Override
//    public Class<PageImpl> handledType() {
//        return PageImpl.class;
//    }
//
////    private String getLink(Optional link) {
////        return link.isPresent() ? ((Link)link.get()).getHref() : "";
////    }
//}