package com.jobapply.application;
import com.jobapply.job.Job;
import com.jobapply.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
@Entity @Table(name="applications",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","job_id"}))
public class ApplicationRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private UserAccount user;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="job_id",nullable=false) private Job job;
 @Column(nullable=false,length=40) private String status="SAVED";
 @Column(name="application_mode",length=30) private String applicationMode="MANUAL";
 @Column(name="submitted_at") private Instant submittedAt;
 @Column(name="interview_date") private LocalDate interviewDate;
 @Column(name="follow_up_date") private LocalDate followUpDate;
 @Column(columnDefinition="TEXT") private String notes;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;} public UserAccount getUser(){return user;} public void setUser(UserAccount v){user=v;}
 public Job getJob(){return job;} public void setJob(Job v){job=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public String getApplicationMode(){return applicationMode;} public void setApplicationMode(String v){applicationMode=v;}
 public Instant getSubmittedAt(){return submittedAt;} public void setSubmittedAt(Instant v){submittedAt=v;}
 public LocalDate getInterviewDate(){return interviewDate;} public void setInterviewDate(LocalDate v){interviewDate=v;}
 public LocalDate getFollowUpDate(){return followUpDate;} public void setFollowUpDate(LocalDate v){followUpDate=v;}
 public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public Instant getCreatedAt(){return createdAt;}
}
