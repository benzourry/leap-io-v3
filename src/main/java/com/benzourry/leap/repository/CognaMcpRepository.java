package com.benzourry.leap.repository;

import com.benzourry.leap.model.CognaMcp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

//import java.awt.print.Pageable;


@Repository
public interface CognaMcpRepository extends JpaRepository<CognaMcp, Long> {
    @Query("select s from CognaMcp s where s.cogna.id = :cognaId")
    Page<CognaMcp> findByCognaId(@Param("cognaId") long cognaId,
                              Pageable pageable);

}
