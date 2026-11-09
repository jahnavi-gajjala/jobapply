package com.jobapply.job;
import com.jobapply.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/v1/jobs")
public class JobController {
 private final JobRepository jobs; private final UserRepository users;
 public JobController(JobRepository jobs,UserRepository users){this.jobs=jobs;this.users=users;}
 record JobRequest(@NotBlank @Size(max=150) String companyName,
                   @NotBlank @Size(max=200) String jobTitle,
                   @Size(max=20000) String jobDescription,
                   @Size(max=150) String location,
                   @Size(max=50) String jobType,
                   @Size(max=50) String experienceLevel,
                   Integer graduationYear,
                   @Size(max=1000) String applicationUrl){}
 @GetMapping public List<Map<String,Object>> list(@RequestParam(required=false) String q,Authentication auth){
   List<Job> all=auth==null?jobs.findByStatusAndOwnerIsNullOrderByCreatedAtDesc("ACTIVE"):jobs.findVisibleToUser(auth.getName());
   if(q!=null&&!q.isBlank()){String term=q.toLowerCase();all=all.stream().filter(j->j.getJobTitle().toLowerCase().contains(term)||j.getCompanyName().toLowerCase().contains(term)||(j.getLocation()!=null&&j.getLocation().toLowerCase().contains(term))).toList();}
   return all.stream().map(this::view).toList();
 }
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable Long id,Authentication auth){
   Job j=auth==null?jobs.findPublicById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)):jobs.findVisibleById(id,auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
   return view(j);
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> create(@Valid @RequestBody JobRequest r,Authentication auth){
   Job j=new Job();j.setOwner(users.findByEmail(auth.getName()).orElseThrow());
   j.setCompanyName(r.companyName().trim());j.setJobTitle(r.jobTitle().trim());j.setJobDescription(r.jobDescription());
   j.setLocation(r.location());if(r.jobType()!=null)j.setJobType(r.jobType());if(r.experienceLevel()!=null)j.setExperienceLevel(r.experienceLevel());
   j.setGraduationYear(r.graduationYear());j.setApplicationUrl(r.applicationUrl());j.setSource("USER_ADDED");
   return view(jobs.save(j));
 }
 private Map<String,Object> view(Job j){
  return Map.of("id",j.getId(),"companyName",j.getCompanyName(),"jobTitle",j.getJobTitle(),
   "jobDescription",j.getJobDescription()==null?"":j.getJobDescription(),"location",j.getLocation()==null?"":j.getLocation(),
   "jobType",j.getJobType(),"experienceLevel",j.getExperienceLevel(),
   "graduationYear",j.getGraduationYear()==null?0:j.getGraduationYear(),
   "applicationUrl",j.getApplicationUrl()==null?"":j.getApplicationUrl(),"source",j.getSource());
 }
}
