package com.jobapply.application;
import com.jobapply.job.JobRepository;
import com.jobapply.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/v1/applications")
public class ApplicationController {
 private final ApplicationRepository applications; private final JobRepository jobs; private final UserRepository users;
 private static final Set<String> STATUSES=Set.of("SAVED","PREPARING","PENDING_APPROVAL","SUBMITTED","UNDER_REVIEW","INTERVIEW","REJECTED","OFFER","WITHDRAWN");
 public ApplicationController(ApplicationRepository a,JobRepository j,UserRepository u){applications=a;jobs=j;users=u;}
 record CreateRequest(@NotNull Long jobId){}
 record StatusRequest(@NotNull String status){}
 @PostMapping public Map<String,Object> create(@Valid @RequestBody CreateRequest r,Authentication auth){
  String email=auth.getName();
  if(applications.existsByUserEmailAndJobId(email,r.jobId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"You already have an application record for this job");
  var user=users.findByEmail(email).orElseThrow();var job=jobs.findVisibleById(r.jobId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found"));
  var a=new ApplicationRecord();a.setUser(user);a.setJob(job);a.setStatus("SAVED");return view(applications.save(a));
 }
 @GetMapping public List<Map<String,Object>> list(Authentication auth){return applications.findByUserEmailOrderByCreatedAtDesc(auth.getName()).stream().map(this::view).toList();}
 @PatchMapping("/{id}/status") public Map<String,Object> status(@PathVariable Long id,@Valid @RequestBody StatusRequest r,Authentication auth){
  String next=r.status().toUpperCase();
  if(!STATUSES.contains(next))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported application status");
  var a=applications.findByIdAndUserEmail(id,auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(next.equals("SUBMITTED") && a.getSubmittedAt()==null) a.setSubmittedAt(Instant.now());
  a.setStatus(next);return view(applications.save(a));
 }
 @GetMapping("/stats") public Map<String,Object> stats(Authentication auth){
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
   "status",a.getStatus(),"applicationMode",a.getApplicationMode(),
   "submittedAt",a.getSubmittedAt()==null?"":a.getSubmittedAt().toString(),"createdAt",a.getCreatedAt().toString());
 }
}
