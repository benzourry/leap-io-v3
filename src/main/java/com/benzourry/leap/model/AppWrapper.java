package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppWrapper {

    App app;
    String baseIo;
    String baseUi;

    @Builder.Default
    List<Lookup> lookups = Collections.emptyList();

    @Builder.Default
    List<EmailTemplate> mailers = Collections.emptyList();

    @Builder.Default
    List<Endpoint> endpoints = Collections.emptyList();

    @Builder.Default
    List<UserGroup> roles = Collections.emptyList();

    @Builder.Default
    List<Bucket> buckets = Collections.emptyList();

    @Builder.Default
    List<Lambda> lambdas = Collections.emptyList();

    @Builder.Default
    List<Cogna> cognas = Collections.emptyList();

    @Builder.Default
    List<Form> forms = Collections.emptyList();

    @Builder.Default
    List<Dataset> datasets = Collections.emptyList();

    @Builder.Default
    List<Dashboard> dashboards = Collections.emptyList();

    @Builder.Default
    List<Screen> screens = Collections.emptyList();

    @Builder.Default
    List<NaviGroup> navis = Collections.emptyList();

    @Builder.Default
    List<Schedule> schedules = Collections.emptyList();

    @Builder.Default
    Map<Long, List<LookupEntry>> lookupEntries = Collections.emptyMap();

}
