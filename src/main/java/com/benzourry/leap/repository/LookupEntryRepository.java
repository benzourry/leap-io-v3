package com.benzourry.leap.repository;

import com.benzourry.leap.model.LookupEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LookupEntryRepository extends JpaRepository<LookupEntry, Long> {

//    NEW SB 2.7.5 cannot use IS NULL to construct smart-condition since @Param will convert NULL to ''
    // most likely affected native query with is null on :parameter
    @Query(value = "select * from lookup_entry where lookup = :id" +
            " and (:extra is null OR :extra = '' OR (extra like :extra))" +
            " and (:code is null OR :code = '' OR (code like :code))" +
            " and (:name is null OR :name = '' OR (name like :name))" +
            " and (:searchText is null OR :searchText = '' OR (extra like :searchText OR code like :searchText OR name like :searchText OR data like :searchText))",
            nativeQuery = true)
    Page<LookupEntry> findByLookupId(@Param("id") long id, @Param("searchText") String searchText, @Param("code") String code, @Param("name") String name, @Param("extra") String extra, Pageable pageable);


    @Query(value = "select * from lookup_entry where lookup = :id" +
            " and (:extra is null OR :extra = '' OR (extra like :extra))" +
            " and (:code is null OR :code = '' OR (code like :code))" +
            " and (:name is null OR :name = '' OR (`name` like :name))" +
            " and (:searchText is null OR :searchText = '' OR (extra like :searchText OR code like :searchText OR `name` like :searchText OR `data` like :searchText))" +
            " and (enabled = TRUE)",
            nativeQuery = true)
    Page<LookupEntry> findByLookupIdEnabled(@Param("id") long id, @Param("searchText") String searchText, @Param("code") String code, @Param("name") String name, @Param("extra") String extra, Pageable pageable);

//    @Query("select new Map(le.code, le.name) from LookupEntry le where le.lookup.id = :id")
//    Map<String,String> findAsMapByLookupId(@Param("id") long id);


    @Transactional
    @Modifying
    @Query("delete from LookupEntry s where s.lookup.id = :lookupId")
    void deleteByLookupId(@Param("lookupId") Long lookupId);

}
