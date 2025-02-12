package com.benzourry.leap.service;

import com.benzourry.leap.repository.DynamicSQLRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SqlService {

//    EntryRepository entryRepository;
//
//    MailService mailService;

    DynamicSQLRepository dynamicSQLRepository;

    public SqlService(
//            EntryRepository entryRepository,
                      DynamicSQLRepository dynamicSQLRepository
//                      MailService mailService
    ){
//        this.entryRepository = entryRepository;
//        this.mailService = mailService;
        this.dynamicSQLRepository = dynamicSQLRepository;
    }

//    public void checkLargeEntry() {
//        String[] columns = "id,size,email,created_date,created_by,modified_date,modified_by,form_id,form_title,app_id,app_title".split(",");
//        this.entryRepository.findLargeEntry(80000l, PageRequest.of(0, Integer.MAX_VALUE)).stream()
//                .forEach(e -> {
//
//                });
//        mailService.sendMail("system_" + Constant.LEAP_MAILER, new String[]{"blmrazif@unimas.my"}, null, null,
//                "[LEAP Diagnostic] - Large entry detected",
//                """
//                        Dear Admin,
//                        REKA has detected entry with large data size as follows:
//
//                        """);
//    }

//    FOR LAMBDA
    public List select(String query, boolean nativeQuery) throws Exception {
        return this.dynamicSQLRepository.runQueryAsMap(query, Map.of(), nativeQuery);
    }

//    FOR LAMBDA
    public List select(String query, Map<String, Object> params, boolean nativeQuery) throws Exception {
        return this.dynamicSQLRepository.runQueryAsMap(query, params, nativeQuery);
    }

//    FOR LAMBDA
    public long count(String query, boolean nativeQuery) throws Exception {
        return this.dynamicSQLRepository.getQueryCount(query, Map.of(),nativeQuery);
    }

//    FOR LAMBDA
    public long count(String query, Map<String, Object> params, boolean nativeQuery) throws Exception {
        return this.dynamicSQLRepository.getQueryCount(query, params,nativeQuery);
    }

//    FOR LAMBDA
    public List select(String query, Map<String, Object> params,boolean nativeQuery, Pageable pageable) throws Exception {
        return this.dynamicSQLRepository.runPagedQueryAsMap(query, params, nativeQuery, pageable);
    }

//    FOR LAMBDA
    public List select(String query, boolean nativeQuery, Pageable pageable) throws Exception {
        return this.dynamicSQLRepository.runPagedQueryAsMap(query, Map.of(), nativeQuery, pageable);
    }

    @Transactional
    public int exec(String sql, Map<String, Object> params) throws Exception {
        return this.dynamicSQLRepository.executeQuery(sql, params);
    }

}
