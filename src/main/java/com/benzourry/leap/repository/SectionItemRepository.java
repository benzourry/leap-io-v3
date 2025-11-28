package com.benzourry.leap.repository;

import com.benzourry.leap.model.SectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionItemRepository extends JpaRepository<SectionItem, Long> {

    @Query("select s from SectionItem s where s.section.id=:sectionId and s.code=:code")
    SectionItem findBySectionIdAndCode(@Param("sectionId")long sectionId, @Param("code")String code);

    @Query("select si from SectionItem si " +
            " left join si.section s " +
            " left join s.form f " +
            " where f.id=:formId and si.code=:code")
    SectionItem findByFormIdAndCode(@Param("formId")long formId, @Param("code")String code);


//    @Modifying
//    @Query("delete from SectionItem s where s.code = :code AND s.section.form.id = :formId")
//    void deleteByCodeAndFormId(@Param("code") String code,@Param("formId") Long formId);

//    @Query("select s from Section s where s.form.id = :formId")
//    Page<Section> findByFormId(@Param("formId") long formId, Pageable pageable);
}
