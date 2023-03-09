package com.benzourry.leap.service;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class LookupService {
    final LookupRepository lookupRepository;

    final AppRepository appRepository;

    final LookupEntryRepository lookupEntryRepository;

    final UserRepository userRepository;

    final AccessTokenService accessTokenService;

    final EntryRepository entryRepository;

    final ItemRepository itemRepository;

    final SectionItemRepository sectionItemRepository;

    final TierRepository tierRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public LookupService(LookupRepository lookupRepository,
                         AppRepository appRepository,
                         LookupEntryRepository lookupEntryRepository,
                         UserRepository userRepository,
                         AccessTokenService accessTokenService,
                         EntryRepository entryRepository,
                         SectionItemRepository sectionItemRepository,
                         TierRepository tierRepository,
                         ItemRepository itemRepository) {
        this.lookupRepository = lookupRepository;
        this.appRepository = appRepository;
        this.lookupEntryRepository = lookupEntryRepository;
        this.userRepository = userRepository;
        this.accessTokenService = accessTokenService;
        this.entryRepository = entryRepository;
        this.sectionItemRepository = sectionItemRepository;
        this.itemRepository = itemRepository;
        this.tierRepository = tierRepository;
    }

    public Lookup save(Lookup lookup, Long appId, String email) {
        App app = appRepository.getReferenceById(appId);
        lookup.setEmail(email);
        lookup.setApp(app);
        return lookupRepository.save(lookup);
    }

    public LookupEntry save(long id, LookupEntry lookup) {
        Lookup l = lookupRepository.getReferenceById(id);
        lookup.setLookup(l);
        if (l.isDataEnabled()) {
            lookup.setData(lookup.getData());
        }
        return lookupEntryRepository.save(lookup);
    }

//    public Page<Lookup> findAllLookup(Pageable pageable) {
//        return lookupRepository.findAll(pageable);
//    }

//    public record LookupEntryResult(List<LookupEntry> content, long totalElements, long numberOfElements,
//                                    long totalPages, long size, String statusCode) {
//    }

    @Retryable(value = RuntimeException.class)
    public Map<String, Object> findAllEntry(long id, String searchText, HttpServletRequest parameter, boolean onlyEnabled, Pageable pageable) throws IOException {
        Optional<Lookup> lookupOpt = lookupRepository.findById(id);
        Map<String, Object> data = new HashMap<>();

        if (searchText != null) {
            searchText = "%" + searchText.toUpperCase() + "%";
        }

        ObjectMapper mapper = new ObjectMapper();

        RestTemplate rt = new RestTemplate();


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
                    param = parameter.getParameterMap().entrySet().stream()
                            .filter(e -> !lookup.getEndpoint().contains("{" + e.getKey() + "}")) // add param only if not specified in url
                            .filter(e -> !"postBody".equals(e.getKey())) // add param only if not postBody
                            .map(e -> e.getKey() + "=" + e.getValue()[0])
                            .collect(Collectors.joining("&"));
                }

                String dm = lookup.getEndpoint().contains("?") ? "&" : "?";
                String fullUrl = lookup.getEndpoint() + dm + param;

                if (parameter != null) {
                    for (Map.Entry<String, String[]> entry : parameter.getParameterMap().entrySet()) {
                        fullUrl = fullUrl.replace("{" + entry.getKey() + "}", parameter.getParameter(entry.getKey()));
                    }
                }

                JsonNode postBody = null;
                String postBodyStr = parameter.getParameter("postBody");
                System.out.println("postBodyStr:"+parameter.getParameter("postBody"));
                if (parameter!=null && parameter.getParameter("postBody")!=null){
                    postBody = mapper.readTree(parameter.getParameter("postBody"));
                }

                //  System.out.println(fullUrl);

                HttpHeaders headers = new HttpHeaders();

                // utk tambah access token
                if (lookup.isAuth()) {
                    String accessToken = null;

                    if ("authorization".equals(lookup.getAuthFlow())) {
                        UserPrincipal userP = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                        if (userP != null) {
                            User user = userRepository.getReferenceById(userP.getId());
                            accessToken = user.getProviderToken();
                        }
                    } else {
//                        System.out.println("get accesstoken");
                        accessToken = accessTokenService.getAccessToken(lookup.getTokenEndpoint(), lookup.getClientId(), lookup.getClientSecret());
//                        System.out.println("accesstoken:"+accessToken);
                    }
//                    String dm2 = fullUrl.contains("?") ? "&" : "?";
//                    fullUrl = fullUrl + dm2 + "access_token=" + accessToken;

                    if ("url".equals(lookup.getTokenTo())) {
                        // Should have the toggle for token in url vs in header
                        String dm2 = fullUrl.contains("?") ? "&" : "?";
                        fullUrl = fullUrl + dm2 + "access_token=" + accessToken;
                    } else {
                        headers.set("Authorization", "Bearer " + accessToken);
                    }
                }

//                System.out.println("after auth");

                boolean dataEnabled = lookup.isDataEnabled();
                String dataFields = lookup.getDataFields();
                final List<String> dataFieldList = new ArrayList<>();
                boolean hasDataFields = !Helper.isNullOrEmpty(dataFields);
                if (hasDataFields) {
                    dataFieldList.addAll(Arrays.asList(dataFields.split(",")));
                }

                if (lookup.getHeaders() != null && !lookup.getHeaders().isEmpty()) {
                    String[] h1 = lookup.getHeaders().split(Pattern.quote("|"));
                    Arrays.stream(h1).forEach(h -> {
                        String[] h2 = h.split(Pattern.quote("->"));
                        headers.set(h2[0], h2.length > 1 ? h2[1] : null);
                    });
                }

//                System.out.println("v:"+postBodyStr);
//                System.out.println("m:"+lookup.getMethod());
                HttpEntity<Object> entity = new HttpEntity<>(postBody!=null?postBody:"parameters", headers);

                ResponseEntity<String> re = null;

                // If error, the clear Access Token if lookup authenticated
                try {
                    re = rt.exchange(fullUrl, "POST".equals(lookup.getMethod())?HttpMethod.POST:HttpMethod.GET, entity, String.class);
                } catch (Exception e) {
                    if (lookup.isAuth()) {
                        accessTokenService.clearAccessToken(lookup.getClientId() + ":" + lookup.getClientSecret());
                    }

                    System.out.println("LookupService.findAllEntry():"+e.getMessage());
                    throw e;
                }
                JsonNode root;
//                System.out.println("after excahnge%%%%%VVXXXX#####:::"+ re.getStatusCodeValue() + ":::"+ re.getBody());

                if ("json".equals(lookup.getResponseType())) {

                    root = mapper.readTree(re.getBody());

                } else if ("jsonp".equals(lookup.getResponseType())) {
                    String json = re.getBody();
                    String h = json.substring(json.indexOf("(") + 1, json.lastIndexOf(")"));

                    root = mapper.readTree(h);

                } else {
                    root = mapper.readTree("{}");
                }

                //  System.out.println("STATUS CODE:"+re.getStatusCodeValue());

                if (re.getStatusCode() == HttpStatus.OK) {
                    JsonNode list = root.at(lookup.getJsonRoot());
                    String codeProp = Optional.ofNullable(lookup.getCodeProp()).orElse("/code");
                    String descProp = Optional.ofNullable(lookup.getDescProp()).orElse("/name");
                    Optional<String> extraProp = Optional.ofNullable(lookup.getExtraProp());

                    List b = new ArrayList();

                    if (list.isArray()) {
                        b = StreamSupport.stream(list.spliterator(), true)
                                .map(onode -> {
                                    LookupEntry le = new LookupEntry();
                                    le.setCode(onode.at(codeProp).asText());
                                    le.setName(onode.at(descProp).asText());
                                    extraProp.ifPresent(s -> le.setExtra(onode.at(s).asText()));
                                    if (dataEnabled) {
                                        if (hasDataFields) {
                                            le.setData(((ObjectNode) onode).retain(dataFieldList));
                                        } else {
                                            le.setData(onode);
                                        }
                                    }

                                    return le;
                                }) // props.stream().collect(Collectors.toMap(c->"code",c->onode.get(lookup.getCodeProp()).asText())))
                                .collect(Collectors.toList());
                    }

                    data.put("content", b);

                } else {
                    //    System.out.println("STATUS CODE:"+re.getStatusCodeValue());
                    if (lookup.isAuth() && re.getStatusCodeValue() == 401) {
                        accessTokenService.clearAccessToken(lookup.getClientId() + ":" + lookup.getClientSecret());
                    }
                    data.put("statusCode", re.getStatusCode());
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
                if (parameter!=null){
                    code = parameter.getParameter("code");
                    name = parameter.getParameter("name");
                    extra = parameter.getParameter("extra");
//                    addData = parameter.getParameter("addData");
                }
                if (onlyEnabled) {
                    entryList = lookupEntryRepository.findByLookupIdEnabled(lookup.getId(), searchText,code, name, extra, defSort);
                } else {
//                    System.out.println("byId+"+parameter.getParameter("code"));
//                    System.out.println("searchtext:"+searchText);
//                    System.out.println("code:"+parameter.getParameter("code"));
//                    System.out.println("name:"+parameter.getParameter("name"));
//                    System.out.println("extra:"+parameter.getParameter("extra"));
                    entryList = lookupEntryRepository.findByLookupId(lookup.getId(), searchText, code, name, extra, defSort);
//                    entryList = lookupEntryRepository.findByLookupIdNew(lookup.getId(),  parameter.getParameter("code"), parameter.getParameter("name"), parameter.getParameter("extra"), PageRequest.of(0,20));
                }
                data.put("content", entryList.getContent());
                data.put("totalElements", entryList.getTotalElements());
                data.put("numberOfElements", entryList.getNumberOfElements());
                data.put("totalPages", entryList.getTotalPages());
                data.put("size", entryList.getSize());
            }

        }


        return data;
    }

//    public List<Map> findIdByFormId(Long formId) {
//        return lookupRepository.findIdByFormId(formId);
//    }

    public List<Map> findIdByFormIdAndSectionType(Long formId, List<String> sectionType) {
        return lookupRepository.findIdByFormIdAndSectionType(formId, sectionType);
    }

    public void removeLookup(long id) {
        lookupEntryRepository.deleteByLookupId(id);
        lookupRepository.deleteById(id);
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
        return lookupRepository.getReferenceById(id);
    }


    public LookupEntry updateLookupEntry(Long entryId, JsonNode obj) {
        LookupEntry le = lookupEntryRepository.getReferenceById(entryId);

        ObjectMapper mapper = new ObjectMapper();
        String code = obj.at("/code").asText(null);
        String name = obj.at("/name").asText(null);
        String extra = obj.at("/extra").asText(null);
        Integer enabled = null;
        if (obj.at("/enabled").isInt()) {
            enabled = obj.at("/enabled").asInt();
        }

        if (code != null) {
            le.setCode(code);
        }
        if (name != null) {
            le.setName(name);
        }
        if (extra != null) {
            le.setExtra(extra);
        }
        if (enabled != null) {
            le.setEnabled(enabled);
        }

        JsonNode data = obj.at("/data");
        if (!data.isEmpty()) {
            Map<String, Object> oriDataMap = mapper.convertValue(le.getData(), Map.class);
            Map<String, Object> newDataMap = mapper.convertValue(data, Map.class);
            Map<String, Object> merged = new HashMap<>(oriDataMap);
            merged.putAll(newDataMap);
            le.setData(mapper.valueToTree(merged));
        }

        return lookupEntryRepository.save(le);
    }

    public List<Map<String, Long>> saveOrder(List<Map<String, Long>> lookupOrderList) {
        for (Map<String, Long> element : lookupOrderList) {
            LookupEntry fi = lookupEntryRepository.findById(element.get("id")).get();
            fi.setOrdering(element.get("sortOrder"));
            lookupEntryRepository.save(fi);
        }
        return lookupOrderList;
    }

//    @Async("asyncExec")
//    @Transactional(readOnly = true)
//    public void updateLookupData(Long lookupId) throws IOException {
//
//        /***
//         * If source==db,
//         * compare with id and replace by id
//         * If source==rest
//         * compare with by and replace by code
//         */
//
//        ObjectMapper mapper = new ObjectMapper();
//
//        List<Item> itemList = itemRepository.findByDatasourceId(lookupId);
//
//        Lookup lookup = lookupRepository.getReferenceById(lookupId);
//
//        Map<String, LookupEntry> newLEntryMap = new HashMap<>();
//        List<LookupEntry> ler = (List<LookupEntry>) findAllEntry(lookupId, "%", null, true, PageRequest.of(0, Integer.MAX_VALUE)).get("content");
//        ler.forEach(le -> {
//            newLEntryMap.put("db".equals(lookup.getSourceType()) ? le.getId() + "" : le.getCode(), le);
//        });
//
//        itemList.forEach(i -> {
//            Long formId = i.getForm().getId();
//
//            SectionItem si = sectionItemRepository.findByFormIdAndCode(formId, i.getCode());
//            Section s = si.getSection();
//
//            String at = "";
////            if ("list".equals(s.getType())) {
////                at += "/" + s.getCode();
////            }
//
//            try (Stream<Entry> entryStream = entryRepository.findByFormId(formId)) {
//                entryStream.forEach(e -> {
//                    List<JsonNode> nodes = new ArrayList<>();
//
//
//                    if ("list".equals(s.getType())) {
//                        System.out.println("list");
//                        if (e.getData().get(s.getCode())!=null && !e.getData().get(s.getCode()).isNull() && e.getData().get(s.getCode()).isArray()) {
//                            for (JsonNode jn : e.getData().get(s.getCode())) {
//                                nodes.add(jn);
//                            }
//                        }
//                    } else if ("section".equals(s.getType())) {
//                        System.out.println("section");
//                        nodes.add(e.getData());
//                    } else if ("approval".equals(s.getType())){
//                        List<Tier> tlist = tierRepository.findBySectionId(s.getId());
//                        tlist.forEach(t->{
//                            nodes.add(e.getApproval().get(t.getId()).getData());
//                        });
//                    }
//
//                    System.out.println("%%%%:"+nodes);
//
//                    if (nodes.size() > 0) {
//                        // if field ada value & !null and field ada id
//                        nodes.forEach(node -> {
//                            if (node.get(i.getCode()) != null && !node.get(i.getCode()).isNull()) {
//                                // if source==db, check lookup dlm entry ada /id, n dlm lookupentry baru ada /id
//                                if ("db".equals(lookup.getSourceType()) && !node.get(i.getCode()).at("/id").isNull()
//                                        && newLEntryMap.get(node.get(i.getCode()).at("/id").asText()) != null) {
//                                    LookupEntry le = newLEntryMap.get(node.get(i.getCode()).at("/id").asText());
//                                    ObjectNode o = (ObjectNode) node;
//                                    //list:::{test:{code:A,name:A}}
//                                    //section::{test:{}}
//                                    o.set(i.getCode(), mapper.valueToTree(le));
//
//                                    ///// (/section.code/idx/i.code, JsonNode)
////                                    entryRepository.updateDataFieldScope(e.getId(),"/sicode/i.code",mapper.valueToTree(le).toString());
//                                    entryRepository.updateDataField(e.getId(), o.toString());
//                                    System.out.println(o);
//                                }
//                                // if source==rest, check lookup dlm entry ada /code, n dlm lookupentry baru ada /code
//                                if ("rest".equals(lookup.getSourceType()) && !node.get(i.getCode()).at("/code").isNull()
//                                        && newLEntryMap.get(node.get(i.getCode()).at("/code").asText()) != null) {
//                                    LookupEntry le = newLEntryMap.get(node.get(i.getCode()).at("/code").asText());
//                                    ObjectNode o = (ObjectNode) node;
//                                    o.set(i.getCode(), mapper.valueToTree(le));
//                                    entryRepository.updateDataField(e.getId(), o.toString());
//                                }
////                                if ("section".equals(s.getType())){
////
////                                }
//
//                            }
//                        });
//                    }
//                    this.entityManager.detach(e);
//                });
//            }
//
//        });
////        foreach itemList:i {
////            formId = i.getForm().getId();
////            entryList = findByFormId( < id >, {i.getCode() + ".code":})
////        }
////
////
////        Map<String, Lookupentry> newLookupMap = new HashMap<>;
////        List<LookupEntry> lentryList = findLookupEntryByLookupId(lookupId);
////        lentryList.forEach(le -> {
////
////        })
////
////
////        entryList = findByFormId( < id >, {i.getCode() + ".code":value});
////
////        entryList.forEach(e -> {
////            e.put(i.getCode(), newMap.get(i.getCode()))
////        })
//    }


    @Async("asyncExec")
    @Transactional(readOnly = true)
    public void updateLookupDataNew(Long lookupId, String refCol) throws IOException {

        /***
         * If source==db,
         * compare with id and replace by id
         * If source==rest
         * compare with code and replace by code
         */

        ObjectMapper mapper = new ObjectMapper();

        List<Item> itemList = itemRepository.findByDatasourceId(lookupId);

        Lookup lookup = lookupRepository.getReferenceById(lookupId);

        Map<String, LookupEntry> newLEntryMap = new HashMap<>();
        List<LookupEntry> ler = (List<LookupEntry>) findAllEntry(lookupId, "%", null, true, PageRequest.of(0, Integer.MAX_VALUE)).get("content");
        ler.forEach(le -> {
            JsonNode jnode = mapper.valueToTree(le);
//            newLEntryMap.put("db".equals(lookup.getSourceType()) ? le.getId() + "" : le.getCode(), le);
            newLEntryMap.put(jnode.at(refCol).asText(), le);
        });

        itemList.forEach(i -> {
            Long formId = i.getForm().getId();
            System.out.println(i.getLabel()+","+i.getForm().getTitle());

            SectionItem si = sectionItemRepository.findByFormIdAndCode(formId, i.getCode());

            if (si!=null) {
//            System.out.println(formId + ","+ i.getCode() + ":" + si);
//            System.out.println(si.getSection());
                Section s = si.getSection();

                try (Stream<Entry> entryStream = entryRepository.findByFormId(formId)) {
                    entryStream.forEach(e -> {
                        Map<String, JsonNode> nodeMap = new HashMap<>();

                        if ("list".equals(s.getType())) {
                            // Utk list, get List and update each item @ $.<section_key>[index]
                            if (e.getData().get(s.getCode()) != null && !e.getData().get(s.getCode()).isNull() && e.getData().get(s.getCode()).isArray()) {

                                for (int z = 0; z < e.getData().get(s.getCode()).size(); z++) {
                                    JsonNode jn = e.getData().get(s.getCode()).get(z);


                                    if (List.of("checkboxOption").contains(i.getType()) || List.of("multiple").contains(i.getSubType())) {
                                        // multiple lookup inside section
                                        if (jn.get(i.getCode()) != null && !jn.get(i.getCode()).isNull() && jn.get(i.getCode()).isArray()) {
                                            // if really multiple lookup
                                            for (int x = 0; x < jn.get(i.getCode()).size(); x++) {
                                                JsonNode xn = jn.get(i.getCode()).get(x);
                                                nodeMap.put("$." + s.getCode() + "[" + z + "]." + i.getCode() + "[" + x + "]", xn);
//                                            nodeMap.put("$."+i.getCode()+"["+z+"]",jn);
                                            }
                                        }
                                    } else {
                                        //if lookup biasa dlm section
                                        nodeMap.put("$." + s.getCode() + "[" + z + "]." + i.getCode(), jn.get(i.getCode()));
//                                    nodeMap.put("$."+i.getCode(),e.getData().get(i.getCode()));
                                    }
                                }
                            }
                        } else if ("section".equals(s.getType())) {
                            if (List.of("checkboxOption").contains(i.getType()) || List.of("multiple").contains(i.getSubType())) {
                                if (e.getData().get(i.getCode()) != null && !e.getData().get(i.getCode()).isNull() && e.getData().get(i.getCode()).isArray()) {
                                    // if really multiple lookup
                                    for (int z = 0; z < e.getData().get(i.getCode()).size(); z++) {
                                        JsonNode jn = e.getData().get(i.getCode()).get(z);
                                        nodeMap.put("$." + i.getCode() + "[" + z + "]", jn);
                                    }
                                }
                            } else {
                                //if lookup biasa
                                System.out.println("normal section");
                                nodeMap.put("$." + i.getCode(), e.getData().get(i.getCode()));
                            }
                        } else if ("approval".equals(s.getType())) {
                            List<Tier> tlist = tierRepository.findBySectionId(s.getId());
                            tlist.forEach(t -> {
                                if (e.getApproval() != null && e.getApproval().get(t.getId()) != null) {
                                    JsonNode jn = e.getApproval().get(t.getId()).getData();
                                    if (List.of("checkboxOption").contains(i.getType()) || List.of("multiple").contains(i.getSubType())) {
                                        // multiple lookup inside section
                                        if (jn.get(i.getCode()) != null && !jn.get(i.getCode()).isNull() && jn.get(i.getCode()).isArray()) {
                                            // if really multiple lookup
                                            for (int x = 0; x < jn.get(i.getCode()).size(); x++) {
                                                JsonNode xn = jn.get(i.getCode()).get(x);
                                                nodeMap.put(t.getId() + "##$." + i.getCode() + "[" + x + "]", xn);
                                            }
                                        }
                                    } else {
//                                    System.out.println(t.getId() + "##$.");
                                        //if lookup biasa dlm section
                                        nodeMap.put(t.getId() + "##$." + i.getCode(), jn.get(i.getCode()));
                                    }
                                }
                            });
                        }

                        if (nodeMap.size() > 0) {
                            // if field ada value & !null and field ada id
                            nodeMap.forEach((key, node) -> {
                                if (node != null && !node.isNull()) {
                                    // if source==db, check lookup dlm entry ada /id, n dlm lookupentry baru ada /id
                                    if ("db".equals(lookup.getSourceType()) && !node.at(refCol).isNull()
                                            && newLEntryMap.get(node.at(refCol).asText()) != null) {
                                        LookupEntry le = newLEntryMap.get(node.at(refCol).asText());
                                        if ("approval".equals(s.getType())) {
                                            String[] splitted = key.split("##");
                                            if (splitted.length == 2) {
                                                entryRepository.updateApprovalDataFieldScope(e.getId(), Long.parseLong(splitted[0]), splitted[1], "[" + mapper.valueToTree(le).toString() + "]");
                                            }
                                        } else {
                                            entryRepository.updateDataFieldScope(e.getId(), key, "[" + mapper.valueToTree(le).toString() + "]");
                                        }

                                    }
                                    // if source==rest, check lookup dlm entry ada /code, n dlm lookupentry baru ada /code
                                    if ("rest".equals(lookup.getSourceType()) && !node.at(refCol).isNull()
                                            && newLEntryMap.get(node.at(refCol).asText()) != null) {
                                        LookupEntry le = newLEntryMap.get(node.at(refCol).asText());
                                        if ("approval".equals(s.getType())) {
                                            String[] splitted = key.split("##");
                                            if (splitted.length == 2) {
                                                entryRepository.updateApprovalDataFieldScope(e.getId(), Long.parseLong(splitted[0]), splitted[1], "[" + mapper.valueToTree(le).toString() + "]");
                                            }
                                        } else {
                                            entryRepository.updateDataFieldScope(e.getId(), key, "[" + mapper.valueToTree(le).toString() + "]");
                                        }
                                    }
                                    // if note is object, then run, if node is text
                                    // if node !=null // already checked

//                                if (node.isTextual() && node.asText()!=null){
//                                    LookupEntry le = newLEntryMap.get(node.asText());
//                                }
                                    LookupEntry le = null;

                                    if (!node.at(refCol).isNull() && newLEntryMap.get(node.at(refCol).asText()) != null) {
                                        le = newLEntryMap.get(node.at(refCol).asText());

                                    } else if (node.isTextual() && node.asText() != null) {
                                        le = newLEntryMap.get(node.asText());
                                    }

                                    if (le != null) {
                                        if ("approval".equals(s.getType())) {
                                            String[] splitted = key.split("##");
                                            if (splitted.length == 2) {
                                                entryRepository.updateApprovalDataFieldScope(e.getId(), Long.parseLong(splitted[0]), splitted[1], "[" + mapper.valueToTree(le).toString() + "]");
                                            }
                                        } else {
                                            entryRepository.updateDataFieldScope(e.getId(), key, "[" + mapper.valueToTree(le).toString() + "]");
                                        }
                                    }
                                }
                            });
                        }
                        this.entityManager.detach(e);
                    });
                }

            }

        });
    }


//    Map<String,AccessToken> accessToken = new HashMap<>();

//    @Cacheable(value = "access_token")

    /**
     * THE PROBLEM IF MULTIPLE ACCESS_TOKEN CRETAED AT THE SAME TIME WILL BE DUPLICATE TOKEN ERROR FROM OAUTH SERVER
     */
//    public String getAccessTokenOld(String tokenEndpoint, String clientId, String clientSecret){
//        String pair = clientId+":"+clientSecret;
//        AccessToken t = accessToken.get(pair);
//        // if expiry in 30 sec, request for new token
//        if (t!=null && ((System.currentTimeMillis()/1000)+30)<t.getExpiry_time()){
////            AccessToken t = accessToken.get(pair);
////            System.out.println("expiry_time:"+t.getExpiry_time()+", current:"+System.currentTimeMillis()/1000 + ", expires_in:"+ t.getExpires_in());
//            return t.getAccess_token();
//        }else{
////            System.out.println("tokenEndpoint:"+tokenEndpoint+",clientId="+clientId+",clientSecret="+clientSecret);
//            RestTemplate tokenRt = new RestTemplate();
//            HttpHeaders headers = new HttpHeaders();
//            String basic = Base64Utils.encodeToString(pair.getBytes());
//            headers.set("Authorization", "Basic " + basic);
//            HttpEntity<String> entity = new HttpEntity<>(headers);
//
//            ResponseEntity<AccessToken> re = tokenRt.exchange(tokenEndpoint + "?grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret,
//                    HttpMethod.POST, entity, AccessToken.class);
//
//            AccessToken at = re.getBody();
//            at.setExpiry_time((System.currentTimeMillis()/1000)+at.getExpires_in());
//            this.accessToken.put(pair, at);
//
//            return  at.getAccess_token();
//        }
//
//    }

//    public Map<String, String> findAllEntryAsMap(long id, Pageable pageable) {
//        List<LookupEntry> data = lookupEntryRepository.findByLookupId(id, pageable).getData();
//
//        data.stream()
//    }
}
