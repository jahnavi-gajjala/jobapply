package com.jobapply.resume;
import com.jobapply.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="resumes")
public class Resume {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private UserAccount user;
 @Column(nullable=false,length=180) private String title;
 @Column(name="original_file_name",nullable=false,length=255) private String originalFileName;
 @Column(name="content_type",nullable=false,length=120) private String contentType;
 @Column(name="extracted_text",nullable=false,columnDefinition="LONGTEXT") private String extractedText;
 @Column(name="is_master",nullable=false) private boolean master=true;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;} public UserAccount getUser(){return user;} public void setUser(UserAccount v){user=v;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getOriginalFileName(){return originalFileName;} public void setOriginalFileName(String v){originalFileName=v;}
 public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
 public String getExtractedText(){return extractedText;} public void setExtractedText(String v){extractedText=v;}
 public boolean isMaster(){return master;} public void setMaster(boolean v){master=v;}
 public Instant getCreatedAt(){return createdAt;}
}
