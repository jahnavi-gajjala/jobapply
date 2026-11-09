package com.jobapply.resume;
import com.jobapply.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/api/v1/resumes")
public class ResumeController {
 private final ResumeRepository resumes; private final UserRepository users; private final ResumeParserService parser;
 public ResumeController(ResumeRepository r,UserRepository u,ResumeParserService p){resumes=r;users=u;parser=p;}
 @PostMapping(value="/upload",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> upload(@RequestParam("file") MultipartFile file,@RequestParam(required=false) String title,Authentication auth){
  if(file.isEmpty()||file.getSize()>5*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Upload a non-empty PDF or DOCX up to 5 MB");
  String name=file.getOriginalFilename()==null?"resume":file.getOriginalFilename();
  if(!(name.toLowerCase().endsWith(".pdf")||name.toLowerCase().endsWith(".docx")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only PDF and DOCX files are supported");
  try{
   String text=parser.extract(file);
   if(text.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No readable text found. Scanned PDF OCR is not included yet.");
   Resume r=new Resume();r.setUser(users.findByEmail(auth.getName()).orElseThrow());r.setTitle(title==null||title.isBlank()?name:title.trim());
   r.setOriginalFileName(name);r.setContentType(file.getContentType()==null?"application/octet-stream":file.getContentType());
   r.setExtractedText(text);r.setMaster(resumes.findByUserEmailOrderByCreatedAtDesc(auth.getName()).isEmpty());resumes.save(r);
   return Map.of("id",r.getId(),"title",r.getTitle(),"fileName",name,"isMaster",r.isMaster(),"characterCount",text.length());
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Could not parse this document");}
 }
 @GetMapping public List<Map<String,Object>> list(Authentication auth){return resumes.findByUserEmailOrderByCreatedAtDesc(auth.getName()).stream().map(r->Map.<String,Object>of("id",r.getId(),"title",r.getTitle(),"fileName",r.getOriginalFileName(),"isMaster",r.isMaster(),"characterCount",r.getExtractedText().length(),"createdAt",r.getCreatedAt().toString())).toList();}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable Long id,Authentication auth){Resume r=resumes.findByIdAndUserEmail(id,auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));return Map.of("id",r.getId(),"title",r.getTitle(),"fileName",r.getOriginalFileName(),"extractedText",r.getExtractedText(),"isMaster",r.isMaster());}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id,Authentication auth){resumes.delete(resumes.findByIdAndUserEmail(id,auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));}
}
