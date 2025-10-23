package com.benzourry.leap.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contract_info")
@Setter
@Getter
public class KryptaContractInfo {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String contractAddress;
//    private Long networkId;

    private String rpcUrl;
    private Long chainId; //1337L for dev

    // getters & setters
}
