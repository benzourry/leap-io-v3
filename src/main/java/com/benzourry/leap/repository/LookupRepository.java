package com.benzourry.leap.repository;

import com.benzourry.leap.model.Lookup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface LookupRepository extends JpaRepository<Lookup, Long> {

//    @Query("select new map(fi.code as code, fi.type as type, fi.dataSource as dataSource, fi.dataSourceInit as dataSourceInit) from Form f " +
////            " left join f.sections fs " +
//            " left join f.items fi " +
//            " where f.id = :formId and fi.dataSource is not null")
//    List<Map> findIdByFormId(@Param("formId") Long formId);

//    @Query(value = "select new map(i.datasource as dataSource, i.data_source_init as dataSourceInit,i.type as type, s.id as sectionId, i.code as code) from form f " +
//            "left join section s on s.form = f.id " +
//            "left join section_item si on si.section = s.id " +
//            "left join item i on si.code = i.code and i.form = f.id " +
//            "where f.id = :formId and s.type in :sectionType and i.datasource is not null", nativeQuery = true)



    //    @Query(value = "select new map(i.code as code,i.type as type, json_value(i.x,'$.skipLoadSource') as skipLoadSource, i.dataSource as dataSource, i.dataSourceInit as dataSourceInit,s.id as sectionId) from Form f " +
//            "left join Section s on s.form.id = f.id " +
//            "left join SectionItem si on si.section.id = s.id " +
//            "left join Item i on si.code = i.code and i.form.id = f.id " +
//            "where f.id = :formId and s.type in :sectionType and i.dataSource is not null " +
//            "and i.type not in ('dataset','screen') ")

    @Query(value = "select i.code as code, i.type as type, " +
            "case json_value(i.x,'$.skipLoadSource') when '1' then 1 else 0 end as skipLoadSource, " +
            "i.datasource as dataSource, i.data_source_init as dataSourceInit, " +
            "s.id as sectionId " +
            "from form f " +
            "left join `section` s on s.form = f.id " +
            "left join section_item si on si.section = s.id " +
            "left join item i on si.code = i.code and i.form = f.id " +
            "where f.id = :formId and s.type in (:sectionType) and i.datasource is not null " +
            "and i.type not in ('dataset','screen')", nativeQuery = true)
    List<Map> findIdByFormIdAndSectionType(@Param("formId") Long formId,
                                           @Param("sectionType") List<String> sectionType);

    @Query("select l from Lookup l where " +
            " (l.shared=TRUE OR l.email = :email) " +
            " AND (upper(l.name) like :searchText or upper(l.description) like :searchText)")
    Page<Lookup> findByQuery(@Param("searchText") String searchText,
                             @Param("email") String email,
                             Pageable pageable);

    @Query("select l from Lookup l where l.app.id = :appId" +
            " AND (upper(l.name) like :searchText or upper(l.description) like :searchText)")
    Page<Lookup> findByAppId(@Param("searchText") String searchText,
                             @Param("appId") long appId, Pageable pageable);

//    @Query(value = "select * from lookup l " +
//            " left join app_user au on l.access = au.user_group " +
//            " left join users u on au.user = u.id " +
////            " left join user_group access on l.access = access.id " +
//            " where l.app = :appId" +
//            " and (l.access is null or (u.email = :email AND au.status = 'approved'))" +
//            " AND (upper(l.name) like :searchText or upper(l.description) like :searchText)", nativeQuery = true)
//    Page<Lookup> findByAppIdAndEmail(@Param("searchText") String searchText,@Param("appId") long appId,@Param("email") String email, Pageable pageable);


//    @Query(value = "select l from Lookup l " +
//            " left join l.access access " +
//            " left join AppUser au on access.id = au.group.id " +
//            " left join au.user u " +
////            " left join user_group access on l.access = access.id " +
//            " where l.id in (:ids) " +
//            " and (access.id is null or (u.email = :email AND au.status = 'approved'))" +
//            " AND (upper(l.name) like :searchText or upper(l.description) like :searchText)")
    @Query(value = """
            select l from Lookup l 
             left JOIN AppUser au on function('find_in_set',au.group.id,l.accessList)<>0 
             left join au.user u 
             where l.id in (:ids) 
             and (l.accessList is null or l.accessList = '' or (u.email = :email AND au.status = 'approved'))
             and (upper(l.name) like :searchText or upper(l.description) like :searchText)
            """)
    Page<Lookup> findByIdsAndEmail(@Param("searchText") String searchText,
                                   @Param("ids") List<Long> ids,
                                   @Param("email") String email, Pageable pageable);

    @Query("select count(l.id) from Lookup l where l.app.id = :appId")
    long countByAppId(@Param("appId") long appId);

    @Modifying
    @Query("delete from Lookup s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);


}
