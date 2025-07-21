package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppWrapper {
    App app;
    String baseIo;
    String baseUi;
    List<Lookup> lookups = new ArrayList<>();
    List<EmailTemplate> mailers = new ArrayList<>();
    List<Endpoint> endpoints = new ArrayList<>();
    List<UserGroup> roles = new ArrayList<>();
    List<Bucket> buckets = new ArrayList<>();
    List<Lambda> lambdas = new ArrayList<>();
    List<Cogna> cognas = new ArrayList<>();
    List<Form> forms = new ArrayList<>();
    List<Dataset> datasets = new ArrayList<>();
    List<Dashboard> dashboards = new ArrayList<>();
    List<Screen> screens = new ArrayList<>();
    List<NaviGroup> navis = new ArrayList<>();
    List<Schedule> schedules = new ArrayList<>();
    Map<Long, List<LookupEntry>> lookupEntries = new HashMap<>();
}
