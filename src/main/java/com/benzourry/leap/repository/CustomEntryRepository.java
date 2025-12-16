package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


public interface CustomEntryRepository {
    Stream<Entry> streamAll(Specification<Entry> spec);

    Page<EntryDto> findPaged(Specification<Entry> spec, Map<String, Set<String>> fields, boolean includeApproval, Pageable pageable);

    Page<EntryDto> findDataPaged(Specification<Entry> spec, Map<String, Set<String>> fields, Pageable pageable);

    Page<Long> findAllIds(Specification<Entry> spec, Pageable pageable);

}
