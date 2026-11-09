package com.jobapply.ai;
import com.jobapply.job.JobRepository;
import com.jobapply.resume.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import java.io.ByteArrayOutputStream;
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
  String tailored=tailorText(r.getExtractedText(),j.getJobTitle(),j.getJobDescription());
  return Map.of("jobTitle",j.getJobTitle(),"company",j.getCompanyName(),"tailoredText",tailored,"changes","Reordered existing resume lines by job-description overlap. No new qualifications or experience were invented.","requiresReview",true,"note","Keyword-prioritized draft only; it is not a formatted final resume.");
 }
 @PostMapping("/tailor-resume/export/docx")
 public ResponseEntity<byte[]> exportTailoredDocx(@RequestBody DraftRequest req,Authentication auth){
  var r=resumes.findByIdAndUserEmail(req.resumeId(),auth.getName())
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume not found"));
  var j=jobs.findVisibleById(req.jobId(),auth.getName())
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found"));
  String tailored=tailorText(r.getExtractedText(),j.getJobTitle(),j.getJobDescription());
  try(XWPFDocument document=new XWPFDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
   XWPFParagraph heading=document.createParagraph();
   heading.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
   XWPFRun title=heading.createRun();title.setBold(true);title.setFontSize(18);title.setText("RESUME");
   XWPFParagraph target=document.createParagraph();
   target.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
   XWPFRun subtitle=target.createRun();subtitle.setFontSize(10);subtitle.setColor("555555");
   subtitle.setText("Draft tailored for "+j.getJobTitle()+" — "+j.getCompanyName());
   for(String line:tailored.split("\\R")){
    XWPFParagraph paragraph=document.createParagraph();
    XWPFRun run=paragraph.createRun();run.setFontSize(10);run.setText(line);
   }
   document.write(output);
   HttpHeaders headers=new HttpHeaders();
   headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
   headers.setContentDispositionFormData("attachment","jobapply-tailored-resume.docx");
   headers.setContentLength(output.size());
   return new ResponseEntity<>(output.toByteArray(),headers,HttpStatus.OK);
  }catch(Exception ex){
   throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not generate the DOCX resume");
  }
 }
 private String tailorText(String source,String jobTitle,String description){
  Set<String> terms=Pattern.compile("[a-zA-Z][a-zA-Z0-9+#.]{1,}")
   .matcher((jobTitle+" "+(description==null?"":description)).toLowerCase())
   .results().map(m->m.group()).collect(java.util.stream.Collectors.toSet());
  return Arrays.stream(source.split("\\R")).map(String::trim).filter(x->!x.isBlank())
   .sorted(Comparator.comparingInt((String line)->(int)terms.stream().filter(line.toLowerCase()::contains).count()).reversed())
   .collect(java.util.stream.Collectors.joining("\\n"));
 }
 @PostMapping("/generate-cover-letter") public Map<String,Object> cover(@RequestBody DraftRequest req,Authentication auth){
  var r=resumes.findByIdAndUserEmail(req.resumeId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  var j=jobs.findVisibleById(req.jobId(),auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  String first=r.getExtractedText().lines().filter(x->!x.isBlank()).findFirst().orElse("I am a motivated candidate");
  String letter="Dear Hiring Team,\n\nI am writing to express my interest in the "+j.getJobTitle()+" opportunity at "+j.getCompanyName()+". "+first+"\n\nI would welcome the opportunity to discuss how my documented skills and project experience align with your team's needs. Thank you for your time and consideration.\n\nSincerely,\n[Your Name]";
  return Map.of("coverLetter",letter,"requiresReview",true,"note","Review every statement and replace placeholders before sending.");
}
}
