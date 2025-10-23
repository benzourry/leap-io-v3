package com.benzourry.leap.repository;

import com.benzourry.leap.model.KryptaContractInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KryptaContractInfoRepository extends JpaRepository<KryptaContractInfo, Long> {}