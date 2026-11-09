package com.jobapply.ats;
import com.jobapply.job.JobRepository;
import com.jobapply.resume.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@RestController @RequestMapping("/api/v1/ats")
public class AtsController {
 private final ResumeRepository resumes;private final JobRepository jobs;
 public AtsController(ResumeRepository r,JobRepository j){resumes=r;jobs=j;}
 record AnalyzeRequest(Long resumeId,Long jobId){}
 private static final Set<String> STOP=Set.of("and","the","for","with","you","your","our","are","will","this","that","from","have","has","into","who","what","when","where","how","job","role","work","team","using","use","ability","strong","good","years","year","must","should","can","not","all","any","their","also","such","more","less","required","preferred","responsibilities","requirements","experience","skills","candidate","company","looking","join","build","develop","support","knowledge");
 @PostMapping("/analyze") public Map<String,Object> analyze(@RequestBody AnalyzeRequest req,Authentication auth){
  var r=resumes.findByIdAndUserEmail(req.resumeId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume not found"));
  var j=jobs.findVisibleById(req.jobId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found"));
  Set<String> rw=words(r.getExtractedText()),jw=words(j.getJobTitle()+" "+(j.getJobDescription()==null?"":j.getJobDescription()));
  Set<String> matched=new TreeSet<>(jw);matched.retainAll(rw);Set<String> missing=new TreeSet<>(jw);missing.removeAll(rw);
  double keyword=jw.isEmpty()?0:100.0*matched.size()/jw.size();String t=r.getExtractedText().toLowerCase();int sections=0;
  for(String x:List.of("education","skills","projects","experience","summary","certifications"))if(t.contains(x))sections++;
  double structure=Math.round(sections/6.0*100);double score=Math.round(keyword*.7+structure*.3);
  List<String> tips=new ArrayList<>();if(!t.contains("projects"))tips.add("Add a Projects section if you have relevant projects.");if(!t.contains("education"))tips.add("Make your Education section easy to locate.");if(!missing.isEmpty())tips.add("Review missing terms and add only those that truthfully describe your skills.");
  return Map.of("overallScore",score,"keywordCoverage",Math.round(keyword*100.0)/100.0,"structureScore",structure,"matchedKeywords",matched.stream().limit(40).toList(),"missingKeywords",missing.stream().limit(40).toList(),"recommendations",tips,"disclaimer","Estimated job-description compatibility, not an employer ATS score or hiring prediction.");
 }
 private Set<String> words(String s){return Pattern.compile("[a-zA-Z][a-zA-Z0-9+#.]{1,}").matcher(s.toLowerCase()).results().map(m->m.group()).filter(w->!STOP.contains(w)).collect(Collectors.toCollection(TreeSet::new));}
}
