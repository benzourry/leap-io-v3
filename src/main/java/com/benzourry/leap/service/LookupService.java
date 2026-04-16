package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.exception.UpstreamServerErrorException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.OptionalBooleanBuilder;
import com.benzourry.leap.utility.TenantLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class LookupService {

    private static final Logger logger = LoggerFactory.getLogger(LookupService.class);
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
    private final HttpClient HTTP_CLIENT;

    private final SecretRepository secretRepository;

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
                         SecretRepository secretRepository,
                         ObjectMapper MAPPER,
                         HttpClient HTTP_CLIENT,
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
        this.secretRepository = secretRepository;
//        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.MAPPER = MAPPER;
        this.HTTP_CLIENT = HTTP_CLIENT;
        this.self = self;
    }

    public Lookup save(Lookup lookup, Long appId, String email) {
        App app = appRepository.getReferenceById(appId); // ok
        lookup.setEmail(email);
        lookup.setApp(app);
        return lookupRepository.save(lookup);
    }

    @Transactional
    public LookupEntry saveLookupEntry(long lookupId, LookupEntry lookupEntry) {
        Lookup l = lookupRepository.findById(lookupId).orElseThrow(() -> new ResourceNotFoundException("Lookup", "id", lookupId));
        lookupEntry.setLookup(l);
//        if (l.isDataEnabled()) {
//            lookupEntry.setData(lookupEntry.getData());
//        }
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
        entryService.resyncEntryData(itemList, refCol, entryDataNode);
    }


//    @Retryable(retryFor = RuntimeException.class)
    public Map<String, Object> findAllEntry(long id, String searchText, HttpServletRequest parameter, boolean onlyEnabled, Pageable pageable) throws IOException {

        Map<String, String> p = new HashMap<>();

        if (parameter != null) {
            parameter.getParameterMap().forEach((key, value) -> {
                p.put(key, value[0]);
            });
        }

        return _findAllEntry(id, searchText, p, onlyEnabled, pageable);
    }

    //FOR LAMBDA
    public Map<String, Object> list(long id, Map<String, Object> param, Lambda lambda) throws Exception {
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
    public Map<String, Object> _findAllEntryOld(long id, String searchText, Map<String, String> parameter, boolean onlyEnabled, Pageable pageable) throws IOException {
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

                HttpResponse<String> response = self.runRestLookup(lookup, parameter);

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
                    if (lookup.isAuth() && response.statusCode() == 401) {
                        accessTokenService.clearAccessToken(lookup.getClientId() + ":" + lookup.getClientSecret());
                    }
                    data.put("statusCode", response.statusCode());
                }
            } else if ("db".equals(lookup.getSourceType())) {
                PageRequest defSort = PageRequest.of(pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSort().isUnsorted() ? Sort.by(Sort.Direction.ASC, "ordering", "id") : pageable.getSort());
                String code = null;
                String name = null;
                String extra = null;
                if (parameter != null) {
                    code = parameter.get("code");
                    name = parameter.get("name");
                    extra = parameter.get("extra");
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

                Page<LookupEntry> entryList = findEntryByParams(lookup, searchText, code, name, extra, onlyEnabled, filtersReq, defSort);

                Map<String, Object> page = Map.of(
                        "totalElements", entryList.getTotalElements(),
                        "number", pageable.getPageNumber(),
                        "numberOfElements", entryList.getNumberOfElements(),
                        "totalPages", entryList.getTotalPages(),
                        "size", entryList.getSize());

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


    public Map<String, Object> _findAllEntry(long id, String searchText, Map<String, String> parameter, boolean onlyEnabled, Pageable pageable) throws IOException {
        Map<String, Object> data = new HashMap<>();
        Optional<Lookup> lookupOpt = lookupRepository.findById(id);

        // 1. Guard clause to eliminate top-level nesting
        if (lookupOpt.isEmpty()) {
            return data;
        }

        Lookup lookupInit = lookupOpt.get();

        // 2. Simplified proxy resolution
        Lookup lookup = "proxy".equals(lookupInit.getSourceType())
                ? lookupRepository.findById(lookupInit.getProxyId()).orElse(lookupInit)
                : lookupInit;

        if ("rest".equals(lookup.getSourceType())) {
            HttpResponse<String> response = self.runRestLookup(lookup, parameter);

            // 3. Handle non-OK statuses early to keep the main logic un-nested
            if (response.statusCode() != HttpStatus.OK.value()) {
                if (lookup.isAuth() && response.statusCode() == 401) {
                    accessTokenService.clearAccessToken(lookup.getClientId() + ":" + lookup.getClientSecret());
                }
                data.put("statusCode", response.statusCode());
                return data;
            }

            String body = response.body();
            String responseType = lookup.getResponseType();

            // 4. Safer JSON string extraction
            if ("jsonp".equals(responseType) && body.contains("(") && body.contains(")")) {
                body = body.substring(body.indexOf("(") + 1, body.lastIndexOf(")"));
            } else if (!"json".equals(responseType) && !"jsonp".equals(responseType)) {
                body = "{}";
            }

            JsonNode root = MAPPER.readTree(body);
            JsonNode list = root.at(lookup.getJsonRoot());

            List<LookupEntry> entries = new ArrayList<>();

            if (list != null && list.isArray()) {
                boolean dataEnabled = lookup.isDataEnabled();
                String dataFields = lookup.getDataFields();
                boolean hasDataFields = !Helper.isNullOrEmpty(dataFields);

                // Streamline data field lists
                List<String> dataFieldList = hasDataFields
                        ? Arrays.stream(dataFields.split(",")).filter(f -> !f.isEmpty()).map(String::trim).toList()
                        : Collections.emptyList();

                List<String[]> splitDataFields = dataFieldList.stream()
                        .map(c -> c.split("@"))
                        .toList();

                String codeProp = Optional.ofNullable(lookup.getCodeProp()).orElse("/code");
                String descProp = Optional.ofNullable(lookup.getDescProp()).orElse("/name");
                String extraProp = Optional.ofNullable(lookup.getExtraProp()).orElse("");

                for (JsonNode onode : list) {
                    LookupEntry le = new LookupEntry();
                    le.setCode(extractJsonValue(onode, codeProp));
                    le.setName(extractJsonValue(onode, descProp));

                    if (!extraProp.isBlank()) {
                        le.setExtra(extractJsonValue(onode, extraProp));
                    }

                    if (dataEnabled) {
                        if (hasDataFields) {
                            ObjectNode on = onode.deepCopy();
                            on.retain(dataFieldList);

                            splitDataFields.forEach(strs -> {
                                String vfield = strs[0].split(":")[0].trim();
                                String sPointer = strs.length == 2
                                        ? strs[1].trim()
                                        : (vfield.startsWith("/") ? vfield : "/" + vfield);

                                JsonNode valueToSet = sPointer.contains("[*]")
                                        ? Helper.jsonAtPath(onode, sPointer)
                                        : onode.at(sPointer);
                                on.set(vfield, valueToSet);
                            });
                            le.setData(on);
                        } else {
                            le.setData(onode);
                        }
                    }
                    entries.add(le);
                }
            }

            // 5. Cleaned up pagination response mapping
            int size = entries.size();
            data.put("content", entries);
            data.put("page", Map.of(
                    "totalElements", size,
                    "number", 0,
                    "numberOfElements", size,
                    "totalPages", 1,
                    "size", size
            ));
            data.put("totalElements", size);
            data.put("number", 0);
            data.put("numberOfElements", size);
            data.put("totalPages", 1);
            data.put("size", size);

        } else if ("db".equals(lookup.getSourceType())) {
            Sort sort = pageable.getSort().isUnsorted() ? Sort.by(Sort.Direction.ASC, "ordering", "id") : pageable.getSort();
            PageRequest defSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

            String code = null;
            String name = null;
            String extra = null;
            Map filtersReq = new HashMap<>();

            if (parameter != null) {
                // Set defaults before the loop
                code = parameter.get("code");
                name = parameter.get("name");
                extra = parameter.get("extra");

                // 6. Simplified parameter parsing loop
                for (Map.Entry<String, String> entry : parameter.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue();

                    // Cache boolean evaluations to avoid duplicate .startsWith() checks
                    boolean isCode = key.startsWith("code");
                    boolean isName = key.startsWith("name");
                    boolean isExtra = key.startsWith("extra");

                    if (isCode) code = val;
                    else if (isName) name = val;
                    else if (isExtra) extra = val;

                    if (isCode || isName || isExtra || key.contains("$") || key.contains("@")) {
                        filtersReq.put(key, val);
                    }                }
            }

            Page<LookupEntry> entryList = findEntryByParams(lookup, searchText, code, name, extra, onlyEnabled, filtersReq, defSort);

            data.put("content", entryList.getContent());
            data.put("page", Map.of(
                    "totalElements", entryList.getTotalElements(),
                    "number", pageable.getPageNumber(),
                    "numberOfElements", entryList.getNumberOfElements(),
                    "totalPages", entryList.getTotalPages(),
                    "size", entryList.getSize()
            ));
            data.put("totalElements", entryList.getTotalElements());
            data.put("number", pageable.getPageNumber());
            data.put("numberOfElements", entryList.getNumberOfElements());
            data.put("totalPages", entryList.getTotalPages());
            data.put("size", entryList.getSize());
        }

        return data;
    }

    @Retryable(
            retryFor = { IOException.class, UpstreamServerErrorException.class, IllegalStateException.class, ConnectException.class },
            noRetryFor = { ResourceNotFoundException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public HttpResponse<String> runRestLookup(Lookup lookup, Map<String, String> parameter) throws IOException {
        // 1. SAFE ATTEMPT TRACKING
        int attempt = Optional.ofNullable(RetrySynchronizationManager.getContext())
                .map(c -> c.getRetryCount() + 1).orElse(1);

        String url = lookup.getEndpoint();
        Long appId = lookup.getAppId();

        // 2. URL & PARAMETER RESOLUTION
        StringJoiner queryJoiner = new StringJoiner("&");
        if (parameter != null) {
            for (Map.Entry<String, String> e : parameter.entrySet()) {
                String key = e.getKey();
                String value = e.getValue() != null ? e.getValue() : "";
                String placeholder = "{" + key + "}";

                if (url.contains(placeholder)) {
                    url = url.replace(placeholder, URLEncoder.encode(value, StandardCharsets.UTF_8));
                } else if (!"postBody".equals(key)) {
                    queryJoiner.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            }
        }

        // Append query strings and cleanup unused placeholders
        if (queryJoiner.length() > 0) {
            url += (url.contains("?") ? "&" : "?") + queryJoiner.toString();
        }
        url = url.replaceAll("\\{.*?\\}", "");

        // 3. RESOLVE SECRETS IN URL
        if (url.contains("_secret")) {
            Map<String, Set<String>> secrets = Helper.extractVariables(Set.of("_secret"), url);
            for (String s : secrets.getOrDefault("_secret", Collections.emptySet())) {
                String value = secretRepository.getValue(appId, s)
                    .orElseThrow(() -> {
                        TenantLogger.error(appId, "lookup", lookup.getId(), "Secret [" + s + "] not found for lookup URL");
                        return new ResourceNotFoundException("Secret", "key+appId", s + "+" + appId);
                    });
                url = url.replace("{_secret." + s + "}", value);
            }
        }

        // 4. BUILD REQUEST & HEADERS
        java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                .header("Content-Type", "application/json;charset=UTF-8");

        if (lookup.getHeaders() != null && !lookup.getHeaders().isEmpty()) {
            for (String h : lookup.getHeaders().split("\\|")) {
                String[] parts = h.split("->");
                if (parts.length >= 1) reqBuilder.setHeader(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
            }
        }

        // 5. CONSOLIDATED AUTHENTICATION LOGIC
        if (lookup.isAuth()) {
            String accessToken = null;
            if ("authorization".equals(lookup.getAuthFlow())) {
                UserPrincipal userP = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                User user = userRepository.findById(userP.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userP.getId()));
                accessToken = user.getProviderToken();
            } else {
                String clientSecret = lookup.getClientSecret();

                if (clientSecret != null && clientSecret.contains("{{_secret.")) {
                    String key = Helper.extractTemplateKey(clientSecret, "{{_secret.", "}}")
                            .orElseThrow(() -> {
                                TenantLogger.error(appId, "lookup", lookup.getId(), "Cannot extract secret key from client secret template");
                                return new RuntimeException("Cannot extract secret key from template");
                            });

                    clientSecret = secretRepository.findByKeyAndAppId(key, lookup.getApp().getId())
                            .map(s -> s.getValue()).orElseThrow(() -> {
                                TenantLogger.error(appId, "lookup", lookup.getId(), "Secret [" + key + "] not found for client secret");
                                return new ResourceNotFoundException("Secret", "key+appId", key + "+" + appId);
                            });

                }
                accessToken = accessTokenService.getAccessToken(lookup.getTokenEndpoint(), lookup.getClientId(), clientSecret);
            }

            // Apply token
            if (accessToken != null) {
                if ("url".equals(lookup.getTokenTo())) {
                    url += (url.contains("?") ? "&" : "?") + "access_token=" + accessToken;
                } else {
                    reqBuilder.setHeader("Authorization", "Bearer " + accessToken);
                }
            }
        }

        // 6. EXECUTION
        try {
            reqBuilder.uri(URI.create(url));
        }catch (IllegalArgumentException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Invalid URI format";
            TenantLogger.error(appId, "lookup", lookup.getId(), "Failed to build request URI: " + errorMsg);
            // CRITICAL FIX: Throw exception to prevent NullPointerException downstream!
            throw new RuntimeException("Invalid lookup endpoint URL: " + url, e);
        }

        if ("POST".equalsIgnoreCase(lookup.getMethod())) {
            String postBody = (parameter != null) ? parameter.get("postBody") : null;
            reqBuilder.POST(java.net.http.HttpRequest.BodyPublishers.ofString(postBody != null ? postBody : "{}"));
        } else {
            reqBuilder.GET();
        }

        HttpRequest request = reqBuilder.build();

        HttpResponse<String> response;

        try{
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            TenantLogger.error(appId, "lookup", lookup.getId(), "Attempt #" + attempt + ": Lookup request interrupted ["+url+"]: " + e.getMessage());
            throw new IllegalStateException("Request interrupted", e);
        } catch (IOException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            TenantLogger.error(appId, "lookup", lookup.getId(), "Attempt #" + attempt + ": Network error [" + url + "]: " + errorMsg);
            throw e;
        }
            // 7. RETRY & ERROR LOGIC
        if (response.statusCode() != 200) {

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                if (lookup.isAuth()) {
                    accessTokenService.clearAccessToken(lookup.getClientId() + ":" + lookup.getClientSecret());
                    if ("authorization".equals(lookup.getAuthFlow())) {
                        SecurityContextHolder.clearContext();
                    }
                    // Throwing RuntimeException here prevents retry because it's not in retryFor
                    TenantLogger.error(appId, "lookup", lookup.getId(), "Attempt #" + attempt + ": Authentication failed with status " + response.statusCode());
                }
                TenantLogger.error(appId, "lookup", lookup.getId(), "Attempt #" + attempt + ": Lookup require authentication, response status: " + response.statusCode());
                throw new UpstreamServerErrorException("Attempt #" + attempt + ": Lookup Auth Failed: " + response.statusCode());
            }

            if (response.statusCode() >= 500) {
                TenantLogger.error(appId, "lookup", lookup.getId(), "Attempt #" + attempt + ": Server error " + response.statusCode() +" [" + url + "]: " + response.body());
                throw new UpstreamServerErrorException("Attempt #" + attempt + ": Server error " + response.statusCode());
            }
            throw new UpstreamServerErrorException("Attempt #" + attempt + ": HTTP [" + url + "] returned " + response.statusCode());
        }

        return response;
    }

    private String extractJsonValue(JsonNode node, String path) {
        return path.contains("[*]")
                ? StreamSupport.stream(Helper.jsonAtPath(node, path).spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.joining(", "))
                : node.at(path).asText();
    }

    public Page<LookupEntry> findEntryByParams(Lookup lookup, String searchText, String code, String name, String extra, boolean onlyEnabled, Map<String, Object> data, Pageable pageable) {

        return lookupEntryRepository.findAll((root, query, cb) -> {

            // 1. Compute search pattern once to avoid redundant string operations
            String searchPattern = searchText != null ? "%" + searchText.toLowerCase(Locale.ROOT) + "%" : null;

            List<Predicate> predicates = new OptionalBooleanBuilder(cb)
                    .notEmptyAnd(searchText,
                            cb.or(
                                    cb.like(cb.lower(root.get("data").as(String.class)), searchPattern),
                                    cb.like(cb.lower(root.get("code")), searchPattern),
                                    cb.like(cb.lower(root.get("name")), searchPattern),
                                    cb.like(cb.lower(root.get("extra")), searchPattern)
                            )
                    )
                    .notNullAnd(lookup, cb.equal(root.get("lookup").get("id"), lookup.getId()))
                    .build();

            if (onlyEnabled) {
                predicates.add(cb.isTrue(root.get("enabled")));
            }

            List<Predicate> paramPredicates = new ArrayList<>();

            if (data != null && !data.isEmpty()) {
                // 2. Safely extract condition without mutating the passed-in Map
                String cond = data.containsKey("@cond") ? String.valueOf(data.get("@cond")) : "AND";
                Path<String> dataRoot = root.get("data");

                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String k = entry.getKey();

                    // Skip the condition key during iteration
                    if ("@cond".equals(k)) continue;

                    String[] splitField = k.split("~");
                    String fieldName = splitField[0];
                    String operator = splitField.length > 1 ? splitField[1] : "default";

                    String filterValue = String.valueOf(entry.getValue());
                    String lowerFilterValue = filterValue.toLowerCase(Locale.ROOT);

                    // 3. Handle Native Columns
                    if (k.startsWith("code") || k.startsWith("name") || k.startsWith("extra")) {
                        Path<String> path = root.get(fieldName);

                        switch (operator) {
                            case "in":
                                paramPredicates.add(cb.lower(path).in((Object[]) lowerFilterValue.split(",")));
                                break;
                            case "notcontain":
                                paramPredicates.add(cb.notLike(cb.lower(path), lowerFilterValue));
                                break;
                            case "contain":
                            case "default":
                            default:
                                paramPredicates.add(cb.like(cb.lower(path), lowerFilterValue));
                                break;
                        }
                    }
                    // 4. Handle JSON Columns
                    else {
                        Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, dataRoot, cb.literal(fieldName));
                        Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, dataRoot, cb.literal(fieldName));

                        switch (operator) {
                            case "from":
                                paramPredicates.add(cb.greaterThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                                break;
                            case "to":
                                paramPredicates.add(cb.lessThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                                break;
                            case "between":
                                String[] time = filterValue.split(",");
                                paramPredicates.add(cb.between(jsonValueDouble, Double.parseDouble(time[0]), Double.parseDouble(time[1])));
                                break;
                            case "in":
                                paramPredicates.add(jsonValueString.in((Object[]) filterValue.split(",")));
                                break;
                            case "notcontain":
                                paramPredicates.add(cb.notLike(cb.lower(jsonValueString), lowerFilterValue));
                                break;
                            case "contain":
                            case "default":
                            default:
                                paramPredicates.add(cb.like(cb.lower(jsonValueString), lowerFilterValue));
                                break;
                        }
                    }
                }

                // 5. Only apply logical wrapping if we actually built predicates
                if (!paramPredicates.isEmpty()) {
                    if ("OR".equals(cond)) {
                        predicates.add(cb.or(paramPredicates.toArray(new Predicate[0])));
                    } else {
                        predicates.add(cb.and(paramPredicates.toArray(new Predicate[0])));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

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

//    @Async("asyncExec") // async will hide the exception from being thrown
    @Transactional(readOnly = true) //why read only???readonly should still work
    public void bulkResyncEntryData_lookup(Long lookupId, String oriRefCol, HttpServletRequest parameter) throws IOException, InterruptedException {

        Set<Item> itemList = new HashSet<>(itemRepository.findByDatasourceId(lookupId));

        Lookup lookup = lookupRepository.findById(lookupId).orElseThrow(() -> new ResourceNotFoundException("Lookup", "id", lookupId));

        Map<String, LookupEntry> newLEntryMap = new HashMap<>();
        List<LookupEntry> ler = (List<LookupEntry>) findAllEntry(lookupId, null, parameter, true, PageRequest.of(0, Integer.MAX_VALUE)).get("content");
        ler.forEach(le -> {
            JsonNode jnode = MAPPER.valueToTree(le);
            // Make sure wujud value kt refCol yg dispecify then baruk add ke newLEntryMap,
            // or else, akan add 'null'=>'value'
            String raw = jnode.path(oriRefCol).asText();
            String key = raw.trim().toLowerCase();
            if (key.isBlank()) {
                TenantLogger.error(lookup.getAppId(), "lookup", lookupId, "Reference column " + oriRefCol + " is blank. Resync will not proceed.");
                throw new IllegalStateException("Reference column " + oriRefCol + " is blank.");
            }
            if (newLEntryMap.containsKey(key)) {
                TenantLogger.error(lookup.getAppId(), "lookup", lookupId, "Duplicate value in reference column " + oriRefCol + ": '" + key + "'. Resync will not proceed to prevent data inconsistency/damage.");
                throw new IllegalStateException(
                        "Duplicate value in reference column " + oriRefCol + ": '" + key + "'"
                );
            }
            newLEntryMap.put(key, le);

            entryService.resyncEntryData(itemList, oriRefCol, jnode);

         });
    }
}
