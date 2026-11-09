package com.jobapply.application;

import com.jobapply.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="application_activity")
public class ApplicationActivity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="application_id",nullable=false) private ApplicationRecord application;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private UserAccount user;
 @Column(name="event_type",nullable=false,length=40) private String eventType;
 @Column(name="old_status",length=40) private String oldStatus;
 @Column(name="new_status",length=40) private String newStatus;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;}
 public ApplicationRecord getApplication(){return application;} public void setApplication(ApplicationRecord v){application=v;}
 public UserAccount getUser(){return user;} public void setUser(UserAccount v){user=v;}
 public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
 public String getOldStatus(){return oldStatus;} public void setOldStatus(String v){oldStatus=v;}
 public String getNewStatus(){return newStatus;} public void setNewStatus(String v){newStatus=v;}
 public Instant getCreatedAt(){return createdAt;}
}
