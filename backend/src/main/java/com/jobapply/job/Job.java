package com.jobapply.job;
import jakarta.persistence.*;
import java.time.Instant;
import com.jobapply.user.UserAccount;
@Entity @Table(name="jobs", uniqueConstraints=@UniqueConstraint(columnNames={"source","external_job_id"}))
public class Job {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="company_name",nullable=false,length=150) private String companyName;
 @Column(name="job_title",nullable=false,length=200) private String jobTitle;
 @Column(name="job_description",columnDefinition="TEXT") private String jobDescription;
 @Column(length=150) private String location;
 @Column(name="job_type",length=50) private String jobType="FULL_TIME";
 @Column(name="experience_level",length=50) private String experienceLevel="FRESHER";
 @Column(name="graduation_year") private Integer graduationYear;
 @Column(name="application_url",length=1000) private String applicationUrl;
 @Column(length=50,nullable=false) private String source="MANUAL";
 @Column(name="external_job_id",length=150) private String externalJobId;
 @Column(nullable=false,length=30) private String status="ACTIVE";
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="owner_user_id") private UserAccount owner;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 public Long getId(){return id;} public String getCompanyName(){return companyName;} public void setCompanyName(String v){companyName=v;}
 public String getJobTitle(){return jobTitle;} public void setJobTitle(String v){jobTitle=v;}
 public String getJobDescription(){return jobDescription;} public void setJobDescription(String v){jobDescription=v;}
 public String getLocation(){return location;} public void setLocation(String v){location=v;}
 public String getJobType(){return jobType;} public void setJobType(String v){jobType=v;}
 public String getExperienceLevel(){return experienceLevel;} public void setExperienceLevel(String v){experienceLevel=v;}
 public Integer getGraduationYear(){return graduationYear;} public void setGraduationYear(Integer v){graduationYear=v;}
 public String getApplicationUrl(){return applicationUrl;} public void setApplicationUrl(String v){applicationUrl=v;}
 public String getSource(){return source;} public void setSource(String v){source=v;}
 public String getExternalJobId(){return externalJobId;} public void setExternalJobId(String v){externalJobId=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public UserAccount getOwner(){return owner;} public void setOwner(UserAccount v){owner=v;}
 public Instant getCreatedAt(){return createdAt;}
}
