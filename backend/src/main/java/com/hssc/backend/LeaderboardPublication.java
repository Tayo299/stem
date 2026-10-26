package com.hssc.backend;

import jakarta.persistence.*;

@Entity
@Table(name="leaderboard_publications")
public class LeaderboardPublication {
    @Id
    @Column(name="exam_id")
    private java.util.UUID examId;
    @Column(nullable=false,columnDefinition="text")
    private String selectedSchoolIds="";
    @Column(nullable=false)
    private boolean published;

    protected LeaderboardPublication() {}
    public LeaderboardPublication(java.util.UUID examId){this.examId=examId;}
    public java.util.UUID getExamId(){return examId;}
    public String getSelectedSchoolIds(){return selectedSchoolIds;}
    public boolean isPublished(){return published;}
    public void publish(java.util.List<java.util.UUID> schoolIds){
        this.selectedSchoolIds=schoolIds.stream().map(java.util.UUID::toString).collect(java.util.stream.Collectors.joining(","));
        this.published=true;
    }
    public java.util.List<java.util.UUID> selectedSchoolIds(){
        if(selectedSchoolIds==null || selectedSchoolIds.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(selectedSchoolIds.split(",")).map(java.util.UUID::fromString).toList();
    }
}
