package com.benzourry.leap.repository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Created by MohdRazif on 4/27/2017.
 */

public interface DynamicSQLRepository {

    List<Object[]> runQuery(String q, Map<String, Object> params, boolean nativeQuery);

    List<Object[]> runQueryAsMap(String q, Map<String, Object> params, boolean nativeQuery);

    List<Map> runPagedQuery(String q, Map<String, Object> params, boolean nativeQuery, Pageable pageable);

    List<Map> runPagedQueryAsMap(String q, Map<String, Object> params, boolean nativeQuery, Pageable pageable);

//    List<Map> runPagedQueryAsMap(String q, Map<String, Object> params, boolean nativeQuery, Pageable pageable) throws Exception;

//    List<Map> runPagedQueryWithParam(String q, boolean nativeQuery, List<IrisReportItemParam> params, Map<String, String[]> req, Pageable pageable) throws Exception;

    int getQueryCount(String q, Map<String, Object> params, boolean nativeQuery);

//    int getQueryCountWithParam(String sql, boolean nativeQuery, List<IrisReportItemParam> params, Map<String, String[]> req);

    int executeQuery(String sql,Map<String, Object> params);

//    int checkQuery(String q, boolean nativeQuery);
}
