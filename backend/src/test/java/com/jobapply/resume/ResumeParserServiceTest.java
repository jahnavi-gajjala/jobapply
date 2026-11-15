package com.jobapply.resume;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeParserServiceTest {

    private final ResumeParserService parser = new ResumeParserService();

    @Test
    void rejectsNonPdfContentRenamedWithPdfExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "this is not a PDF".getBytes()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> parser.extract(file)
        );

        assertTrue(error.getMessage().contains("PDF signature"));
    }

    @Test
    void rejectsNonZipContentRenamedWithDocxExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "this is not a DOCX file".getBytes()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> parser.extract(file)
        );

        assertTrue(error.getMessage().contains("DOCX container"));
    }

    @Test
    void extractsTextFromAValidDocxFile() throws Exception {
        byte[] docxBytes;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Java Backend Developer");
            document.createParagraph().createRun().setText("Spring Boot and MySQL");
            document.write(output);
            docxBytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        String extractedText = parser.extract(file);

        assertTrue(extractedText.contains("Java Backend Developer"));
        assertTrue(extractedText.contains("Spring Boot and MySQL"));
    }

    @Test
    void rejectsUnsupportedFileExtensions() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "plain text resume".getBytes()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> parser.extract(file)
        );

        assertEquals("Only PDF and DOCX files are supported", error.getMessage());
    }
}
