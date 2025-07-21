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

    @Query(value = "select s from Schedule s where s.app.id = :appId")
    List<Schedule> findByAppId(@Param("appId") Long appId);

    @Modifying
    @Query("delete from Schedule s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);

    @Query(value = "select s from Schedule s where s.enabled = 1 and s.clock = :clock and s.app.live = TRUE")
    List<Schedule> findByEnabledAndClock(@Param("clock") String clock);

    @Query(value = "select s from Schedule s where s.clock = :clock ")
    List<Schedule> findByClock(@Param("clock") String clock);
}
