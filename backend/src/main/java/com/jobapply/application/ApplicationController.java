package com.jobapply.application;

import com.jobapply.job.JobRepository;
import com.jobapply.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {
 private final ApplicationRepository applications;
 private final JobRepository jobs;
 private final UserRepository users;
 private final ApplicationActivityRepository activities;
 private static final Set<String> STATUSES=Set.of("SAVED","PREPARING","PENDING_APPROVAL","SUBMITTED","UNDER_REVIEW","INTERVIEW","REJECTED","OFFER","WITHDRAWN");

 public ApplicationController(ApplicationRepository a,JobRepository j,UserRepository u,ApplicationActivityRepository activityRepository){
  applications=a;jobs=j;users=u;activities=activityRepository;
 }
 record CreateRequest(@NotNull Long jobId){}
 record StatusRequest(@NotBlank @Size(max=40) String status){}
 record NotesRequest(@Size(max=4000) String notes){}
 record ScheduleRequest(LocalDate interviewDate,LocalDate followUpDate){}

 @PostMapping
 @Transactional
 public Map<String,Object> create(@Valid @RequestBody CreateRequest r,Authentication auth){
  String email=auth.getName();
  if(applications.existsByUserEmailAndJobId(email,r.jobId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"You already have an application record for this job");
  var user=users.findByEmail(email).orElseThrow();
  var job=jobs.findVisibleById(r.jobId(),email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found"));
  var a=new ApplicationRecord();a.setUser(user);a.setJob(job);a.setStatus("SAVED");
  try{
   var saved=applications.save(a);
   record(saved,"CREATED","","SAVED");
   return view(saved);
  }catch(DataIntegrityViolationException ex){
   throw new ResponseStatusException(HttpStatus.CONFLICT,"You already have an application record for this job");
  }
 }

 @GetMapping
 public List<Map<String,Object>> list(Authentication auth){
  return applications.findByUserEmailOrderByCreatedAtDesc(auth.getName()).stream().map(this::view).toList();
 }

 @PatchMapping("/{id}/status")
 @Transactional
 public Map<String,Object> status(@PathVariable Long id,@Valid @RequestBody StatusRequest r,Authentication auth){
  String next=r.status().trim().toUpperCase();
  if(!STATUSES.contains(next))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported application status");
  var a=owned(id,auth);
  String previous=a.getStatus();
  if(next.equals("SUBMITTED") && a.getSubmittedAt()==null)a.setSubmittedAt(Instant.now());
  a.setStatus(next);
  var saved=applications.save(a);
  if(!next.equals(previous))record(saved,"STATUS_CHANGED",previous,next);
  return view(saved);
 }

 @PutMapping("/{id}/notes")
 @Transactional
 public Map<String,Object> updateNotes(@PathVariable Long id,@Valid @RequestBody NotesRequest request,Authentication auth){
  var a=owned(id,auth);
  String previous=a.getNotes()==null?"":a.getNotes();
  String next=request.notes()==null?"":request.notes().trim();
  a.setNotes(next);
  var saved=applications.save(a);
  if(!previous.equals(next))record(saved,"NOTES_UPDATED","","");
  return view(saved);
 }

 @PutMapping("/{id}/schedule")
 @Transactional
 public Map<String,Object> updateSchedule(@PathVariable Long id,@RequestBody ScheduleRequest request,Authentication auth){
  var a=owned(id,auth);
  if(request.interviewDate()!=null && request.followUpDate()!=null && request.followUpDate().isBefore(request.interviewDate())){
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Follow-up date cannot be before the interview date");
  }
  boolean changed=!Objects.equals(a.getInterviewDate(),request.interviewDate()) || !Objects.equals(a.getFollowUpDate(),request.followUpDate());
  a.setInterviewDate(request.interviewDate());
  a.setFollowUpDate(request.followUpDate());
  var saved=applications.save(a);
  if(changed)record(saved,"SCHEDULE_UPDATED","","");
  return view(saved);
 }

 @GetMapping("/{id}/activity")
 public List<Map<String,Object>> activity(@PathVariable Long id,Authentication auth){
  owned(id,auth);
  return activities.findByApplicationIdAndUserEmailOrderByCreatedAtAsc(id,auth.getName()).stream().map(this::activityView).toList();
 }

 private ApplicationRecord owned(Long id,Authentication auth){
  return applications.findByIdAndUserEmail(id,auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found"));
 }
 private void record(ApplicationRecord application,String type,String oldStatus,String newStatus){
  var event=new ApplicationActivity();
  event.setApplication(application);event.setUser(application.getUser());event.setEventType(type);
  event.setOldStatus(oldStatus);event.setNewStatus(newStatus);activities.save(event);
 }
 private Map<String,Object> activityView(ApplicationActivity event){
  return Map.of("id",event.getId(),"eventType",event.getEventType(),
   "oldStatus",event.getOldStatus()==null?"":event.getOldStatus(),
   "newStatus",event.getNewStatus()==null?"":event.getNewStatus(),
   "createdAt",event.getCreatedAt().toString());
 }

 @GetMapping("/stats")
 public Map<String,Object> stats(Authentication auth){
  String email=auth.getName();
  long total=applications.countByUserEmailAndStatusIn(email,List.of("SUBMITTED","UNDER_REVIEW","INTERVIEW","REJECTED","OFFER"));
  return Map.of("totalApplied",total,
   "underReview",applications.countByUserEmailAndStatusIn(email,List.of("UNDER_REVIEW")),
   "interviews",applications.countByUserEmailAndStatusIn(email,List.of("INTERVIEW")),
   "offers",applications.countByUserEmailAndStatusIn(email,List.of("OFFER")),
   "rejected",applications.countByUserEmailAndStatusIn(email,List.of("REJECTED")),
   "saved",applications.countByUserEmailAndStatusIn(email,List.of("SAVED","PREPARING","PENDING_APPROVAL")));
 }
 private Map<String,Object> view(ApplicationRecord a){
  return Map.of("id",a.getId(),"jobId",a.getJob().getId(),"companyName",a.getJob().getCompanyName(),
   "jobTitle",a.getJob().getJobTitle(),"location",a.getJob().getLocation()==null?"":a.getJob().getLocation(),
   "status",a.getStatus(),"applicationMode",a.getApplicationMode(),"notes",a.getNotes()==null?"":a.getNotes(),
   "submittedAt",a.getSubmittedAt()==null?"":a.getSubmittedAt().toString(),
   "interviewDate",a.getInterviewDate()==null?"":a.getInterviewDate().toString(),
   "followUpDate",a.getFollowUpDate()==null?"":a.getFollowUpDate().toString(),
   "createdAt",a.getCreatedAt().toString());
 }
}
