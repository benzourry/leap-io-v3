package com.benzourry.leap.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.io.Serializable;

/**
 * Created by MohdRazif on 1/8/2016.
 */
@Setter
@Getter
@Entity
//@SQLDelete(sql = "UPDATE key_value SET ACTIVE_FLAG = false WHERE id = ?", check = ResultCheckStyle.COUNT) //hibernate specific
//@Where(clause = "active_flag <> false") //hibernate specific
@Table(name = "KEY_VALUE")
//@IdClass(KVId.class)
public class  KeyValue {

//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

//    @Id
    @Column(name = "K")
    private String key;
//    @Id
    @Column(name = "G")
    private String group;

    @Column(name = "V", length=4000)
    private String value;


    @Column(name = "ENABLED")
    private Integer enabled;



//    @Type(value = JsonType.class)
//    @Column(columnDefinition = "json", name = "V")
//    private JsonNode value;


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

// Why this? Dont know the relevant of this

//@Setter
//@Getter
//@EqualsAndHashCode
//class KVId implements Serializable{
//        String key;
//        String group;
//}