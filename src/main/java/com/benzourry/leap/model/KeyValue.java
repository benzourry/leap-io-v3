package com.benzourry.leap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by MohdRazif on 1/8/2016.
 */
@Setter
@Getter
@Entity
//@SQLDelete(sql = "UPDATE key_value SET ACTIVE_FLAG = false WHERE id = ?", check = ResultCheckStyle.COUNT) //hibernate specific
//@Where(clause = "active_flag <> false") //hibernate specific
@Table(name = "KEY_VALUE")
public class  KeyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "K")
    private String key;

    @Column(name = "G")
    private String group;

    @Column(name = "V", length=4000)
    private String value;

    @Column(name = "ENABLED")
    private Integer enabled;

    public KeyValue(){}

    public KeyValue(String group, String key, String value, Integer enabled){
        this.key = key;
        this.value = value;
        this.group = group;
        this.enabled = enabled;
    }

    public static KeyValue  of (String group, String key, String value, Integer enabled){
        return new KeyValue(group, key, value, enabled);
    }
}
