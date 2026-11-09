package com.jobapply.ai;
import com.jobapply.job.JobRepository;
import com.jobapply.resume.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.regex.Pattern;
@RestController @RequestMapping("/api/v1/ai")
public class AiController {
 private final ResumeRepository resumes;private final JobRepository jobs;
 public AiController(ResumeRepository r,JobRepository j){resumes=r;jobs=j;}
 record DraftRequest(Long resumeId,Long jobId){}
 @PostMapping("/tailor-resume") public Map<String,Object> tailor(@RequestBody DraftRequest req,Authentication auth){
  var r=resumes.findByIdAndUserEmail(req.resumeId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  var j=jobs.findVisibleById(req.jobId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  Set<String> terms=Pattern.compile("[a-zA-Z][a-zA-Z0-9+#.]{1,}").matcher((j.getJobTitle()+" "+(j.getJobDescription()==null?"":j.getJobDescription())).toLowerCase()).results().map(m->m.group()).collect(java.util.stream.Collectors.toSet());
  List<String> lines=Arrays.stream(r.getExtractedText().split("\\R")).map(String::trim).filter(x->!x.isBlank()).sorted(Comparator.comparingInt((String line)->(int)terms.stream().filter(line.toLowerCase()::contains).count()).reversed()).toList();
  return Map.of("jobTitle",j.getJobTitle(),"company",j.getCompanyName(),"tailoredText",String.join("\n",lines),"changes","Reordered existing resume lines by job-description overlap. No new qualifications or experience were invented.","requiresReview",true,"note","Keyword-prioritized draft only; it is not a formatted final resume.");
 }
 @PostMapping("/generate-cover-letter") public Map<String,Object> cover(@RequestBody DraftRequest req,Authentication auth){
  var r=resumes.findByIdAndUserEmail(req.resumeId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  var j=jobs.findVisibleById(req.jobId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  String first=r.getExtractedText().lines().filter(x->!x.isBlank()).findFirst().orElse("I am a motivated candidate");
  String letter="Dear Hiring Team,\n\nI am writing to express my interest in the "+j.getJobTitle()+" opportunity at "+j.getCompanyName()+". "+first+"\n\nI would welcome the opportunity to discuss how my documented skills and project experience align with your team's needs. Thank you for your time and consideration.\n\nSincerely,\n[Your Name]";
  return Map.of("coverLetter",letter,"requiresReview",true,"note","Review every statement and replace placeholders before sending.");
}
}
