package com.benzourry.leap.model;

//import com.benzourry.leap.utility.jsontype.JsonBinaryType;
//import com.benzourry.leap.utility.jsontype.JsonStringType;
//import org.hibernate.annotations.TypeDef;
//import org.hibernate.annotations.TypeDefs;

import jakarta.persistence.MappedSuperclass;

//@TypeDefs({
//        @TypeDef(name = "json", typeClass = JsonStringType.class),
//        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
//})
@MappedSuperclass
public class BaseEntity { }