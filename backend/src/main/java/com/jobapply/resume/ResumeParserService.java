package com.jobapply.resume;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
@Service
public class ResumeParserService {
 public String extract(MultipartFile file) throws Exception {
  String n=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase();
  if(n.endsWith(".pdf")){try(var pdf=Loader.loadPDF(file.getBytes())){return new PDFTextStripper().getText(pdf).trim();}}
  if(n.endsWith(".docx")){try(var doc=new XWPFDocument(new ByteArrayInputStream(file.getBytes()));var ex=new XWPFWordExtractor(doc)){return ex.getText().trim();}}
  throw new IllegalArgumentException("Only PDF and DOCX files are supported");
 }
}
