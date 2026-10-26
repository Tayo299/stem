package com.hssc.backend;
import jakarta.persistence.*;
@Entity @Table(name="leaderboard_policies")
public class LeaderboardPolicy {
    @Id private Integer id=1;
    @Column(nullable=false) private int qualifiersPerDistrict;
    @Column(nullable=false,length=500) private String tieBreakOrder;
    @Column(nullable=false) private boolean configured;
    protected LeaderboardPolicy() {}
    public static LeaderboardPolicy unconfigured(){return new LeaderboardPolicy();}
    public Integer getId(){return id;} public int getQualifiersPerDistrict(){return qualifiersPerDistrict;} public String getTieBreakOrder(){return tieBreakOrder;} public boolean isConfigured(){return configured;}
    public void configure(int qualifiers,String order){this.qualifiersPerDistrict=qualifiers;this.tieBreakOrder=order;this.configured=true;}
}
