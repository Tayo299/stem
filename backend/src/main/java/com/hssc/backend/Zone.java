package com.hssc.backend;
import jakarta.persistence.*;
@Entity @Table(name="zones", uniqueConstraints=@UniqueConstraint(name="uk_zone_district_name", columnNames={"district_id","name"}))
public class Zone {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private java.util.UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="district_id",nullable=false) private District district;
    @Column(nullable=false,length=120) private String name;
    protected Zone() {}
    public Zone(District district,String name){this.district=district;this.name=name;}
    public java.util.UUID getId(){return id;} public District getDistrict(){return district;} public String getName(){return name;}
}
