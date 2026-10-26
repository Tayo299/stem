package com.hssc.backend;
import jakarta.persistence.*;
@Entity @Table(name="districts", uniqueConstraints=@UniqueConstraint(name="uk_district_code", columnNames="code"))
public class District {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @Column(nullable=false,length=32) private String code;
    @Column(nullable=false,length=120) private String name;
    protected District() {}
    public District(String code,String name){this.code=code;this.name=name;}
    public java.util.UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;}
}
