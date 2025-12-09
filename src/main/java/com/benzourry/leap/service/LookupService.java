package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.OptionalBooleanBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

//import org.springframework.util.Base64Utils;

@Service
public class LookupService {
    final LookupRepository lookupRepository;

    final AppRepository appRepository;

    final LookupEntryRepository lookupEntryRepository;

    final UserRepository userRepository;

    final AccessTokenService accessTokenService;

    final EntryRepository entryRepository;

    final EntryService entryService;

    final ItemRepository itemRepository;

    final SectionItemRepository sectionItemRepository;

    final TierRepository tierRepository;

    private final ObjectMapper MAPPER;

    private final LookupService self;


    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();


    public LookupService(LookupRepository lookupRepository,
                         AppRepository appRepository,
                         LookupEntryRepository lookupEntryRepository,
                         UserRepository userRepository,
                         AccessTokenService accessTokenService,
                         EntryRepository entryRepository,
                         EntryService entryService,
                         SectionItemRepository sectionItemRepository,
                         TierRepository tierRepository,
                         ItemRepository itemRepository,
                         PlatformTransactionManager transactionManager,
                         ObjectMapper MAPPER,
                         @Lazy LookupService self) {
        this.lookupRepository = lookupRepository;
        this.appRepository = appRepository;
        this.lookupEntryRepository = lookupEntryRepository;
        this.userRepository = userRepository;
        this.accessTokenService = accessTokenService;
        this.entryRepository = entryRepository;
        this.entryService = entryService;
        this.sectionItemRepository = sectionItemRepository;
        this.itemRepository = itemRepository;
        this.tierRepository = tierRepository;
//        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.MAPPER = MAPPER;
        this.self = self;
    }

    public Lookup save(Lookup lookup, Long appId, String email) {
        App app = appRepository.getReferenceById(appId); // ok
        lookup.setEmail(email);
        lookup.setApp(app);
        return lookupRepository.save(lookup);
    }

    @Transactional
    public LookupEntry save(long id, LookupEntry lookupEntry) {
        Lookup l = lookupRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lookup", "id", id));
        lookupEntry.setLookup(l);
        if (l.isDataEnabled()) {
            lookupEntry.setData(lookupEntry.getData());
        }
        LookupEntry le = lookupEntryRepository.save(lookupEntry);

        if (l.getX() != null && l.getX().at("/autoResync").asBoolean(false)) {
            String refCol = l.getX().at("/refCol").asText("code");
            JsonNode jnode = MAPPER.valueToTree(le);
            self.resyncEntryData_Lookup(l.getId(), refCol, jnode);
        }

        return le;
    }

    @Async("asyncExec")
    @Transactional
    public void resyncEntryData_Lookup(Long lookupId, String refCol, JsonNode entryDataNode) {
        Set<Item> itemList = new HashSet<>(itemRepository.findByDatasourceId(lookupId));
        System.out.println("ITEM LIST::: " + itemList.stream().map(Item::getLabel).toList());
        entryService.resyncEntryData(itemList, refCol, entryDataNode);
    }


    @Retryable(retryFor = RuntimeException.class)
    public Map<String, Object> findAllEntry(long id, String searchText, HttpServletRequest parameter, boolean onlyEnabled, Pageable pageable) throws IOException, InterruptedException, RuntimeException {

        Map<String, String> p = new HashMap<>();

        if (parameter != null) {
            parameter.getParameterMap().forEach((key, value) -> {
                p.put(key, value[0]);
            });
        }

        return _findAllEntry(id, searchText, p, onlyEnabled, pageable);
    }

    //FOR LAMBDA
    public Map<String, Object> list(long id, Map<String, Object> param, Lambda lambda) throws IOException, InterruptedException {
        Object searchTextObj = param.remove("searchText");
        String searchText = null;
        if (searchTextObj != null) {
            searchText = searchTextObj + "";
        }

        int page = 0;
        int size = Integer.MAX_VALUE;

        if (param.get("page") != null) {
            page = (int) param.remove("page");
        }
        if (param.get("size") != null) {
            page = (int) param.remove("size");
        }
        PageRequest pageable = PageRequest.of(page, size);

        boolean onlyEnabled = true;
        if (param.get("onlyEnabled") != null) {
            onlyEnabled = (boolean) param.remove("onlyEnabled");
        }

        Map<String, String> newParam = new HashMap<>();
        for (Map.Entry<String, Object> e : param.entrySet()) {
            newParam.put(e.getKey(), (String) e.getValue());
        }

        /**
         _lookup.list(12,{
         code:'M',
         name:'Male',
         '$.name':'Mohd',
         onlyEnabled: true
         },_this);
         */
        return _findAllEntry(id, searchText, newParam, onlyEnabled, pageable);
    }

    /**
     * @param id
     * @param searchText
     * @param parameter
     * @param onlyEnabled
     * @param pageable
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws RuntimeException
     * @// TODO: 17/3/2025 Try to fix 'Illegal character in query...' when parameter passed directly and contained encoded space '%20'
     */
    public Map<String, Object> _findAllEntry(long id, String searchText, Map<String, String> parameter, boolean onlyEnabled, Pageable pageable) throws IOException, InterruptedException, RuntimeException {
        Optional<Lookup> lookupOpt = lookupRepository.findById(id);
        Map<String, Object> data = new HashMap<>();

        if (lookupOpt.isPresent()) {
            Lookup lookupInit = lookupOpt.get();

            Lookup lookup;

            // if lookup is proxy get value for proxy, if not then just from the lookup itself
            if ("proxy".equals(lookupInit.getSourceType())) {
                Optional<Lookup> lookupProxyOpt = lookupRepository.findById(lookupInit.getProxyId());
                lookup = lookupProxyOpt.orElse(lookupInit);
            } else {
                lookup = lookupInit;
            }

            if ("rest".equals(lookup.getSourceType())) {
//            ResponseEntity<JsonNode> re = rt.getForEntity(lookup.getUrl(),JsonNode.class);
                // Create the request body as a MultiValueMap

                /*
                 * Processiong request
                 * */
                String param = "";
                if (parameter != null) {
                    // add param only if not specified in url
                    // add param only if not postBody
                    StringJoiner joiner = new StringJoiner("&");
                    for (Map.Entry<String, String> e : parameter.entrySet()) {
                        if (!lookup.getEndpoint().contains("{" + e.getKey() + "}")) {
                            if (!"postBody".equals(e.getKey())) {
                                String s = e.getKey() + "=" + e.getValue();
                                joiner.add(s);
                            }
                        }
                    }
                    param = joiner.toString();
                }

                String dm = lookup.getEndpoint().contains("?") ? "&" : "?";
                String fullUrl = lookup.getEndpoint() + dm + param;

                if (parameter != null) {
                    for (Map.Entry<String, String> entry : parameter.entrySet()) {
                        fullUrl = fullUrl.replace("{" + entry.getKey() + "}", URLEncoder.encode(parameter.get(entry.getKey()), StandardCharsets.UTF_8));
                    }
                    //replace remaining with blank
                    fullUrl = fullUrl.replaceAll("\\{.*?\\}", "");
                }

                JsonNode postBody = null;
//                String postBodyStr = parameter.getParameter("postBody");
//                System.out.println("postBodyStr:"+parameter.getParameter("postBody"));
                if (parameter != null && parameter.get("postBody") != null) {
                    postBody = MAPPER.readTree(parameter.get("postBody"));
                }

                java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder();
                HttpResponse<String> response = null;
//                HttpClient httpClient = HttpClient.newBuilder()
//                        .version(HttpClient.Version.HTTP_1_1)
//                        .connectTimeout(Duration.ofSeconds(30))
//                        .build();

                requestBuilder.setHeader("Content-Type", "application/json;charset=UTF-8");
                if (lookup.getHeaders() != null && !lookup.getHeaders().isEmpty()) {
                    String[] h1 = lookup.getHeaders().split(Pattern.quote("|"));
                    Arrays.stream(h1).forEach(h -> {
                        String[] h2 = h.split(Pattern.quote("->"));
                        requestBuilder.setHeader(h2[0], h2.length > 1 ? h2[1] : null);
                    });
                }

                // utk tambah access token
                if (lookup.isAuth()) {
                    String accessToken = null;

                    if ("authorization".equals(lookup.getAuthFlow())) {
                        UserPrincipal userP = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                        if (userP != null) {
                            User user = userRepository.findById(userP.getId()).orElseThrow(() -> new ResourceNotFoundException("User", "id", userP.getId()));
                            accessToken = user.getProviderToken();
                        }
                    } else {
                        accessToken = accessTokenService.getAccessToken(lookup.getTokenEndpoint(), lookup.getClientId(), lookup.getClientSecret());
                    }

                    if ("url".equals(lookup.getTokenTo())) {
                        // Should have the toggle for token in url vs in header
                        String dm2 = fullUrl.contains("?") ? "&" : "?";
                        fullUrl = fullUrl + dm2 + "access_token=" + accessToken;
                    } else {
                        requestBuilder.setHeader("Authorization", "Bearer " + accessToken);
                    }
                }

                try {
                    if ("GET".equals(lookup.getMethod())) {
                        java.net.http.HttpRequest request = requestBuilder
                                .GET()
                                .uri(URI.create(fullUrl))
                                .build();

                        response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    } else if ("POST".equals(lookup.getMethod())) {
                        java.net.http.HttpRequest request = requestBuilder
                                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(postBody)))
                                .uri(URI.create(fullUrl))
                                .build();

                        response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    }
                } catch (Exception e) {
                    if (lookup.isAuth()) {
                        accessTokenService.clearAccessToken(lookup.getClientId() + ":" + lookup.getClientSecret());
                    }
                    System.out.println("LookupService.findAllEntry():" + e.getMessage());
                    throw e;
                }

                JsonNode root;

                if ("json".equals(lookup.getResponseType())) {

                    root = MAPPER.readTree(response.body());

                } else if ("jsonp".equals(lookup.getResponseType())) {
                    String json = response.body();
                    String h = json.substring(json.indexOf("(") + 1, json.lastIndexOf(")"));
                    root = MAPPER.readTree(h);
                } else {
                    root = MAPPER.readTree("{}");
                }

                //  System.out.println("STATUS CODE:"+re.getStatusCodeValue());

                if (response.statusCode() == HttpStatus.OK.value()) {
                    JsonNode list = root.at(lookup.getJsonRoot());

                    boolean dataEnabled = lookup.isDataEnabled();
                    String dataFields = lookup.getDataFields();
                    final List<String> dataFieldList = new ArrayList<>(); // list("name at /data/0/name","age:number at /data/0/age","id")
                    boolean hasDataFields = !Helper.isNullOrEmpty(dataFields);
                    if (hasDataFields) {
                        dataFieldList.addAll(Arrays.stream(dataFields.split(",")).filter(f -> !f.isEmpty()).map(String::trim).toList());
                    }


                    String codeProp = Optional.ofNullable(lookup.getCodeProp()).orElse("/code");
                    String descProp = Optional.ofNullable(lookup.getDescProp()).orElse("/name");
                    Optional<String> extraProp = Optional.ofNullable(lookup.getExtraProp());

                    List<LookupEntry> entries = new ArrayList<>();

                    // list("name@/data/0/name","age:number@/data/0/age","id")
                    // list(["name","/data/0/name"],["age:number","/data/0/age"],["id"])
                    List<String[]> x = new ArrayList<>();
                    for (String c : dataFieldList) {
                        String[] split = c.split("@");
                        x.add(split);
                    }

                    if (list.isArray()) {
                        for (JsonNode onode : list) {

                            LookupEntry le = new LookupEntry();

                            le.setCode(extractJsonValue(onode, codeProp));
                            le.setName(extractJsonValue(onode, descProp));
                            if (extraProp.isPresent() && !extraProp.get().isBlank()) {
                                le.setExtra(extractJsonValue(onode, extraProp.get()));
                            }
                            if (dataEnabled) {
                                // syntax is name:string at data/0/name
                                if (hasDataFields) {

                                    ObjectNode on = onode.deepCopy();
                                    on.retain(dataFieldList);

                                    x.forEach(strs -> {
                                        String vfield = strs[0].split(":")[0].trim(); //name, age, id
                                        String sPointer = vfield
                                                .startsWith("/") ? vfield : "/" + vfield; // /name,/age, /id
                                        if (strs.length == 2) sPointer = strs[1].trim(); // if a:/b/c > /b/c

                                        on.set(vfield,
                                                (sPointer.contains("[*]")) ?
                                                        Helper.jsonAtPath(onode, sPointer)
                                                        : onode.at(sPointer)); // {age: onode.at('/data/0/age')}
                                    });
                                    le.setData(on);
                                } else {
                                    le.setData(onode);
                                }
                            }
                            entries.add(le);
                        }
                    }

                    Map<String, Object> page = Map.of("totalElements", entries.size(),
                            "number", 0,
                            "numberOfElements", entries.size(),
                            "totalPages", 1,
                            "size", entries.size());


                    data.put("content", entries);
                    data.put("page", page);
                    data.put("totalElements", entries.size());
                    data.put("number", 0);
                    data.put("numberOfElements", entries.size());
                    data.put("totalPages", 1);
                    data.put("size", entries.size());

                } else {
                    //    System.out.println("STATUS CODE:"+re.getStatusCodeValue());
                    if (lookup.isAuth() && response.statusCode() == 401) {
                        accessTokenService.clearAccessToken(lookup.getClientId() + ":" + lookup.getClientSecret());
                    }
                    data.put("statusCode", response.statusCode());
                }
            } else if ("db".equals(lookup.getSourceType())) {
                Page<LookupEntry> entryList;
                //  System.out.println(pageable.getSort());
                PageRequest defSort = PageRequest.of(pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSort().isUnsorted() ? Sort.by(Sort.Direction.ASC, "ordering", "id") : pageable.getSort());
                String code = null;
                String name = null;
                String extra = null;
                String addData = null;
                if (parameter != null) {
                    code = parameter.get("code");
                    name = parameter.get("name");
                    extra = parameter.get("extra");
//                    addData = parameter.getParameter("addData");
                }
                Map filtersReq = new HashMap();
                if (parameter != null) {
                    for (Map.Entry<String, String> entry : parameter.entrySet()) {
                        if (entry.getKey().startsWith("code")) {
                            code = entry.getValue();
                        }
                        if (entry.getKey().startsWith("name")) {
                            name = entry.getValue();
                        }
                        if (entry.getKey().startsWith("extra")) {
                            extra = entry.getValue();
                        }
                        if (entry.getKey().contains("$") || entry.getKey().contains("@") ||
                                entry.getKey().startsWith("code") ||
                                entry.getKey().startsWith("name") ||
                                entry.getKey().startsWith("extra")) {
                            filtersReq.put(entry.getKey(), parameter.get(entry.getKey()));
                        }
                    }
                }

                entryList = findEntryByParams(lookup, searchText, code, name, extra, onlyEnabled, filtersReq, defSort);

                Map<String, Object> page = Map.of(
                        "totalElements", entryList.getTotalElements(),
                        "number", pageable.getPageNumber(),
                        "numberOfElements", entryList.getNumberOfElements(),
                        "totalPages", entryList.getTotalPages(),
                        "size", entryList.getSize());

//                new HashMap<>();
//                page.put("totalElements", entryList.getTotalElements());
//                page.put("number", pageable.getPageNumber());
//                page.put("numberOfElements", entryList.getNumberOfElements());
//                page.put("totalPages", entryList.getTotalPages());
//                page.put("size", entryList.getSize());

                data.put("content", entryList.getContent());
                data.put("page", page);
                data.put("totalElements", entryList.getTotalElements());
                data.put("number", pageable.getPageNumber());
                data.put("numberOfElements", entryList.getNumberOfElements());
                data.put("totalPages", entryList.getTotalPages());
                data.put("size", entryList.getSize());

            }

        }

        return data;
    }

    private String extractJsonValue(JsonNode node, String path) {
        return path.contains("[*]")
                ? StreamSupport.stream(Helper.jsonAtPath(node, path).spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.joining(", "))
                : node.at(path).asText();
    }

    public Page<LookupEntry> findEntryByParams(Lookup lookup, String searchText, String code, String name, String extra, boolean onlyEnabled, Map<String, Object> data, Pageable pageable) {

//        System.out.println(searchText);
        return lookupEntryRepository.findAll((root, query, cb) -> {

            String cond = "AND";

            List<Predicate> predicates = new OptionalBooleanBuilder(cb)
                    .notEmptyAnd(searchText,
                            cb.or(cb.like(cb.lower(root.get("data").as(String.class)), searchText != null ? "%" + searchText.toLowerCase(Locale.ROOT) + "%" : null),
                                    cb.like(cb.lower(root.get("code")), searchText != null ? "%" + searchText.toLowerCase(Locale.ROOT) + "%" : null),
                                    cb.like(cb.lower(root.get("name")), searchText != null ? "%" + searchText.toLowerCase(Locale.ROOT) + "%" : null),
                                    cb.like(cb.lower(root.get("extra")), searchText != null ? "%" + searchText.toLowerCase(Locale.ROOT) + "%" : null)
                            )
                    )
                    .notNullAnd(lookup, cb.equal(root.get("lookup").get("id"), lookup.getId()))
//                    .notNullAnd(code, cb.like(cb.lower(root.get("code")), code != null ? code.toLowerCase(Locale.ROOT) : null))
//                    .notNullAnd(name, cb.like(cb.lower(root.get("name")), name != null ? name.toLowerCase(Locale.ROOT) : null))
//                    .notNullAnd(extra, cb.like(cb.lower(root.get("extra")), extra != null ? extra.toLowerCase(Locale.ROOT) : null))
                    .build();

            if (onlyEnabled) {
                predicates.add(cb.isTrue(root.get("enabled")));
            }

            List<Predicate> paramPredicates = new ArrayList<>();


//            if (code != null){
//                paramPredicates.add(cb.like(cb.lower(root.get("code")),code.toLowerCase(Locale.ROOT)));
//            }
//            if (name != null){
//                paramPredicates.add(cb.like(cb.lower(root.get("name")),name.toLowerCase(Locale.ROOT)));
//            }
//            if (extra != null){
//                paramPredicates.add(cb.like(cb.lower(root.get("extra")),extra.toLowerCase(Locale.ROOT)));
//            }

//            Map<String, Object> pF = mapper.convertValue(d.getPresetFilters(), HashMap.class);
            if (data.containsKey("@cond")) {
                cond = data.get("@cond") + "";
                data.remove("@cond");
            }


            if (data != null) {
                Path<?> predRoot = root.get("data");
                data.keySet().forEach(k -> {
//                    System.out.println(k);
                    if (k.startsWith("code") || k.startsWith("name") || k.startsWith("extra")) {
                        String[] splitField = k.split("~");
                        String filterValue = data.get(k).toString();
                        if (splitField.length > 1) {
                            if ("in".equals(splitField[1])) {
                                paramPredicates.add(cb.lower(root.get(splitField[0]))
                                        .in(filterValue.toLowerCase(Locale.ROOT).split(",")));
                            } else if ("contain".equals(splitField[1])) {
                                paramPredicates.add(cb.like(cb.lower(root.get(splitField[0])), filterValue.toLowerCase(Locale.ROOT)));
                            } else if ("notcontain".equals(splitField[1])) {
                                paramPredicates.add(cb.notLike(cb.lower(root.get(splitField[0])), filterValue.toLowerCase(Locale.ROOT)));
                            } else {
                                paramPredicates.add(cb.like(cb.lower(root.get(splitField[0])), filterValue.toLowerCase(Locale.ROOT)));
                            }
                        } else {
                            paramPredicates.add(cb.like(cb.lower(root.get(splitField[0])), filterValue.toLowerCase(Locale.ROOT)));
                        }
                    } else {
//                    if(k.contains("$")) {  // xperlu condition tok sbb dh difilter siap kt method sebelumnya.
                        String[] splitField = k.split("~");
                        String filterValue = data.get(k).toString();
                        if (splitField.length > 1) {
                            if ("from".equals(splitField[1])) {
                                Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, predRoot, cb.literal(splitField[0]));
                                paramPredicates.add(cb.greaterThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                            } else if ("to".equals(splitField[1])) {
                                Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, predRoot, cb.literal(splitField[0]));
                                paramPredicates.add(cb.lessThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));

                            } else if ("between".equals(splitField[1])) {
                                Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, predRoot, cb.literal(splitField[0]));
                                String[] time = filterValue.split(",");
                                paramPredicates.add(cb.between(jsonValueDouble,
                                        Double.parseDouble(time[0]),
                                        Double.parseDouble(time[1])
                                ));
                            } else if ("in".equals(splitField[1])) {
                                Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, predRoot,
                                        cb.literal(splitField[0]));
                                paramPredicates.add(jsonValueString.in((Object[]) filterValue.split(",")));

                            } else if ("contain".equals(splitField[1])) {
                                Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, predRoot,
                                        cb.literal(splitField[0]));
                                paramPredicates.add(cb.like(cb.lower(jsonValueString), filterValue.toLowerCase(Locale.ROOT)));
//                            predicates.add(cb.like(cb.lower(jsonValueString), filterValue.toLowerCase()));
                            } else if ("notcontain".equals(splitField[1])) {
                                Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, predRoot,
                                        cb.literal(splitField[0]));
                                paramPredicates.add(cb.notLike(cb.lower(jsonValueString), filterValue.toLowerCase(Locale.ROOT)));
                            }
                        } else {
                            paramPredicates.add(cb.like(cb.lower(cb.function("JSON_VALUE", String.class,
                                    predRoot,
                                    cb.literal(k))), filterValue.toLowerCase(Locale.ROOT)));
                        }
//                    }
                    }
                });
            }

//            Predicate params;
            if ("OR".equals(cond)) {
                predicates.add(cb.or(paramPredicates.toArray(new Predicate[0])));
            } else {
                predicates.add(cb.and(paramPredicates.toArray(new Predicate[0])));
            }

//            return params;
            return cb.and(predicates.toArray(new Predicate[]{}));
        }, pageable);
    }

//    public List<Map> findIdByFormId(Long formId) {
//        return lookupRepository.findIdByFormId(formId);
//    }

    public List<Map> findIdByFormIdAndSectionType(Long formId, List<String> sectionType) {
        return lookupRepository.findIdByFormIdAndSectionType(formId, sectionType);
    }

    @Transactional
    public void removeLookup(long id) {
        lookupEntryRepository.deleteByLookupId(id);
        lookupRepository.deleteById(id);
    }


    public void clearEntries(long id) {
        lookupEntryRepository.deleteByLookupId(id);
    }


    public void removeLookupEntry(Long id) {
        lookupEntryRepository.deleteById(id);
    }


    public Page<Lookup> findByAppId(String searchText, Long appId, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return lookupRepository.findByAppId(searchText, appId, pageable);
    }

    public Page<Lookup> findByQuery(String searchText, String email, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return lookupRepository.findByQuery(searchText, email, pageable);
    }

    public Lookup getLookup(long id) {
        return lookupRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lookup", "id", id));
    }

    @Transactional
    public LookupEntry updateLookupEntry(Long entryId, JsonNode obj) {
        LookupEntry le = lookupEntryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("LookupEntry", "id", entryId));

        String code = obj.at("/code").asText(null);
        String name = obj.at("/name").asText(null);
        String extra = obj.at("/extra").asText(null);
        Integer enabled = null;
        if (obj.at("/enabled").isInt()) {
            enabled = obj.at("/enabled").asInt();
        }

        if (code != null) le.setCode(code);
        if (name != null) le.setName(name);
        if (extra != null) le.setExtra(extra);
        if (enabled != null) le.setEnabled(enabled);

        JsonNode data = obj.at("/data");
        if (!data.isEmpty()) {
            Map<String, Object> oriDataMap = MAPPER.convertValue(le.getData(), Map.class);
            Map<String, Object> newDataMap = MAPPER.convertValue(data, Map.class);
            Map<String, Object> merged = new HashMap<>(oriDataMap);
            merged.putAll(newDataMap);
            le.setData(MAPPER.valueToTree(merged));
        }
        lookupEntryRepository.save(le);

        Lookup l = le.getLookup();

        if (l != null && l.getX() != null && l.getX().at("/autoResync").asBoolean(false)) {
            String refCol = l.getX().at("/refCol").asText("code");
            JsonNode jnode = MAPPER.valueToTree(le);
            self.resyncEntryData_Lookup(l.getId(), refCol, jnode);
        }

        return le;
    }

    public List<Map<String, Long>> saveOrder(List<Map<String, Long>> lookupOrderList) {
        for (Map<String, Long> element : lookupOrderList) {
            LookupEntry fi = lookupEntryRepository.findById(element.get("id")).get();
            fi.setOrdering(element.get("sortOrder"));
            lookupEntryRepository.save(fi);
        }
        return lookupOrderList;
    }

    @Async("asyncExec") // async will hide the exception from being thrown
    @Transactional(readOnly = true) //why read only???readonly should still work
    public void bulkResyncEntryData_lookup(Long lookupId, String oriRefCol) throws IOException, InterruptedException {

        Set<Item> itemList = new HashSet<>(itemRepository.findByDatasourceId(lookupId));

        lookupRepository.findById(lookupId).orElseThrow(() -> new ResourceNotFoundException("Lookup", "id", lookupId));

        Map<String, LookupEntry> newLEntryMap = new HashMap<>();
        List<LookupEntry> ler = (List<LookupEntry>) findAllEntry(lookupId, null, null, true, PageRequest.of(0, Integer.MAX_VALUE)).get("content");
        ler.forEach(le -> {
            JsonNode jnode = MAPPER.valueToTree(le);
            // Make sure wujud value kt refCol yg dispecify then baruk add ke newLEntryMap,
            // or else, akan add 'null'=>'value'
            String raw = jnode.path(oriRefCol).asText();
            String key = raw.trim().toLowerCase();
            if (key.isBlank()) {
                throw new IllegalStateException("Reference column " + oriRefCol + " is blank.");
            }
            if (newLEntryMap.containsKey(key)) {
                throw new IllegalStateException(
                        "Duplicate value in reference column " + oriRefCol + ": '" + key + "'"
                );
            }
            newLEntryMap.put(key, le);

            entryService.resyncEntryData(itemList, oriRefCol, jnode);

         });
    }
}
