package com.benzourry.leap.repository;

import com.benzourry.leap.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query(value = "select * from schedule s where s.app = :appId", nativeQuery = true)
    List<Schedule> findByAppId(Long appId);

    @Modifying
    @Query("delete from Schedule s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);

    @Query(value = "select * from schedule s where s.enabled = 1 and s.clock = :clock ", nativeQuery = true)
    List<Schedule> findByEnabledAndClock(@Param("clock") String clock);
}
