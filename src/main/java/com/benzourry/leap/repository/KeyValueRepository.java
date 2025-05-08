package com.benzourry.leap.repository;


import com.benzourry.leap.model.KeyValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Created by MohdRazif on 1/8/2016.
 */
@Repository
public interface KeyValueRepository extends JpaRepository<KeyValue, Long> {

    KeyValue findByKey(String key);
    Optional<KeyValue> findByGroupAndKey(String key, String group);

    List<KeyValue> findByGroup(String group);
    @Query("select k.value from KeyValue k where k.key=:key and k.group=:group")
    Optional<String> getValue(@Param("group") String group, @Param("key") String key);
//    KeyValue findById(Long id);

}
