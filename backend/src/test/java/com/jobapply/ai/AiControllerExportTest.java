package com.jobapply.ai;

import com.jobapply.job.Job;
import com.jobapply.job.JobRepository;
import com.jobapply.resume.Resume;
import com.jobapply.resume.ResumeRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiControllerExportTest {

    @Test
    void exportsAnEditableDocxUsingOnlyTheUsersResumeAndVisibleJob() throws Exception {
        ResumeRepository resumes = mock(ResumeRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        Resume resume = new Resume();
        resume.setExtractedText("General profile\nJava and Spring Boot projects\nVolunteer activities");
        Job job = new Job();
        job.setJobTitle("Java Developer");
        job.setCompanyName("Example Tech");
        job.setJobDescription("Java Spring Boot backend development");
        when(resumes.findByIdAndUserEmail(7L, "candidate@example.com")).thenReturn(Optional.of(resume));
        when(jobs.findVisibleById(9L, "candidate@example.com")).thenReturn(Optional.of(job));

        AiController controller = new AiController(resumes, jobs);
        var auth = new UsernamePasswordAuthenticationToken("candidate@example.com", "n/a", List.of());
        var response = controller.exportTailoredDocx(new AiController.DraftRequest(7L, 9L), auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getContentDisposition().isAttachment());
        assertEquals("jobapply-tailored-resume.docx", response.getHeaders().getContentDisposition().getFilename());
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(response.getBody()))) {
            String text = document.getParagraphs().stream()
                    .map(p -> p.getText())
                    .reduce("", (a, b) -> a + "\n" + b);
            assertTrue(text.contains("Java Developer"));
            assertTrue(text.contains("Example Tech"));
            assertTrue(text.contains("Java and Spring Boot projects"));
            assertTrue(text.contains("General profile"));
            assertTrue(text.contains("Volunteer activities"));
        }
        verify(resumes).findByIdAndUserEmail(7L, "candidate@example.com");
        verify(jobs).findVisibleById(9L, "candidate@example.com");
    }

    @Test
    void doesNotExportAResumeOwnedByAnotherUser() {
        ResumeRepository resumes = mock(ResumeRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        when(resumes.findByIdAndUserEmail(7L, "candidate@example.com")).thenReturn(Optional.empty());

        AiController controller = new AiController(resumes, jobs);
        var auth = new UsernamePasswordAuthenticationToken("candidate@example.com", "n/a", List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.exportTailoredDocx(new AiController.DraftRequest(7L, 9L), auth));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(jobs);
    }
}
