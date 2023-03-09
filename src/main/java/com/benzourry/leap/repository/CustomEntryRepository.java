package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;


public interface CustomEntryRepository {
    Stream<Entry> streamAll(Specification<Entry> spec);

    List<JsonNode> findDataPaged(Specification<Entry> spec, Pageable pageable);
}
