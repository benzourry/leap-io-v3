package com.benzourry.leap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "wallet_info")
@Setter
@Getter
public class KryptaWalletInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // network info
    private String rpcUrl;
    private Long chainId; //1337L for dev

    // contract info
    private String contractAddress;

    // wallet info
    private String name;
    private String description;
//    private String address;
//    private String password;
    private String privateKey; // store encrypted value
    private Long userId;
    // getters & setters

    private String email;


    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

}
