package com.jobapply;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapply.job.JobRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobApplyWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JobRepository jobRepository;

    @Test
    void candidateCanCompleteTheCoreJobSearchWorkflowAndDataStaysPrivate() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String ownerEmail = "owner-" + suffix + "@example.com";
        String otherEmail = "other-" + suffix + "@example.com";

        String ownerToken = register(ownerEmail, "Jahnavi Owner");
        String otherToken = register(otherEmail, "Other Candidate");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ownerEmail))
                .andExpect(jsonPath("$.fullName").value("Jahnavi Owner"));

        String jobJson = objectMapper.writeValueAsString(Map.of(
                "companyName", "Example Technologies",
                "jobTitle", "Java Backend Developer Intern",
                "jobDescription", "Java Spring Boot REST API development with MySQL and backend engineering.",
                "location", "Remote",
                "jobType", "INTERNSHIP",
                "experienceLevel", "FRESHER",
                "graduationYear", 2027,
                "applicationUrl", "https://example.com/careers"
        ));

        MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Example Technologies"))
                .andReturn();
        long jobId = objectMapper.readTree(jobResult.getResponse().getContentAsString())
                .path("id").asLong();

        // A second account must not see another candidate's private job.
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        MockMultipartFile invalidPdfFile = new MockMultipartFile(
                "file",
                "renamed-text-file.pdf",
                "application/pdf",
                "this is not a PDF".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(invalidPdfFile)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest());

        MockMultipartFile resumeFile = new MockMultipartFile(
                "file",
                "jahnavi-resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createResumeDocx()
        );
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(resumeFile)
                        .param("title", "Java Backend Resume")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java Backend Resume"))
                .andExpect(jsonPath("$.isMaster").value(true))
                .andReturn();
        long resumeId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("id").asLong();

        mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        MockMultipartFile alternateResumeFile = new MockMultipartFile(
                "file",
                "alternate-resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createResumeDocx()
        );
        MvcResult alternateUploadResult = mockMvc.perform(multipart("/api/v1/resumes/upload")
                        .file(alternateResumeFile)
                        .param("title", "Alternative Resume")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isMaster").value(false))
                .andReturn();
        long alternateResumeId = objectMapper.readTree(alternateUploadResult.getResponse().getContentAsString())
                .path("id").asLong();

        mockMvc.perform(put("/api/v1/resumes/" + alternateResumeId + "/master")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMaster").value(true));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/v1/resumes/" + alternateResumeId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMaster").value(true));

        String analyzeJson = objectMapper.writeValueAsString(Map.of(
                "resumeId", resumeId,
                "jobId", jobId
        ));
        MvcResult atsResult = mockMvc.perform(post("/api/v1/ats/analyze")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(analyzeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.matchedKeywords").isArray())
                .andExpect(jsonPath("$.disclaimer").exists())
                .andReturn();
        JsonNode ats = objectMapper.readTree(atsResult.getResponse().getContentAsString());
        assertTrue(ats.path("overallScore").asInt() > 0);
        assertTrue(ats.path("matchedKeywords").toString().toLowerCase().contains("java"));

        MvcResult tailorResult = mockMvc.perform(post("/api/v1/ai/tailor-resume")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(analyzeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresReview").value(true))
                .andReturn();
        assertTrue(objectMapper.readTree(tailorResult.getResponse().getContentAsString())
                .path("tailoredText").asText().contains("Java"));

        mockMvc.perform(post("/api/v1/ai/generate-cover-letter")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(analyzeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverLetter").value(org.hamcrest.Matchers.containsString("Java Backend Developer Intern")))
                .andExpect(jsonPath("$.requiresReview").value(true));

        String applicationJson = objectMapper.writeValueAsString(Map.of("jobId", jobId));
        MvcResult applicationResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SAVED"))
                .andExpect(jsonPath("$.companyName").value("Example Technologies"))
                .andReturn();
        long applicationId = objectMapper.readTree(applicationResult.getResponse().getContentAsString())
                .path("id").asLong();

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUBMITTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/notes")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "Recruiter contacted me."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Recruiter contacted me."));

        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/schedule")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "interviewDate", "2026-12-01",
                                "followUpDate", "2026-12-05"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewDate").value("2026-12-01"))
                .andExpect(jsonPath("$.followUpDate").value("2026-12-05"));

        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/schedule")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "interviewDate", "2026-12-10",
                                "followUpDate", "2026-12-05"
                        ))))
                .andExpect(status().isBadRequest());

        MvcResult activityResult = mockMvc.perform(get("/api/v1/applications/" + applicationId + "/activity")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andReturn();
        String activityJson = activityResult.getResponse().getContentAsString();
        assertTrue(activityJson.contains("CREATED"));
        assertTrue(activityJson.contains("STATUS_CHANGED"));
        assertTrue(activityJson.contains("NOTES_UPDATED"));
        assertTrue(activityJson.contains("SCHEDULE_UPDATED"));

        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/applications/stats")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplied").value(1))
                .andExpect(jsonPath("$.saved").value(0));
    }

    @Test
    void importedJobicyListingsAreSharedAndCanBeTrackedByDifferentCandidates() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String ownerToken = register("jobicy-owner-" + suffix + "@example.com", "Jobicy Owner");
        String candidateToken = register("jobicy-candidate-" + suffix + "@example.com", "Jobicy Candidate");
        String externalId = String.valueOf(System.currentTimeMillis());

        String importJson = objectMapper.writeValueAsString(Map.of(
                "externalJobId", externalId,
                "companyName", "Northstar Labs",
                "jobTitle", "Java Backend Developer Intern",
                "excerpt", "Build Java REST APIs with Spring Boot and MySQL.",
                "location", "APAC",
                "jobType", "Full-Time, Remote",
                "experienceLevel", "Entry-level",
                "sourceUrl", "https://jobicy.com/jobs/java-backend-developer-intern"
        ));

        MvcResult importResult = mockMvc.perform(post("/api/v1/jobs/import")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("JOBICY"))
                .andExpect(jsonPath("$.externalJobId").value(externalId))
                .andExpect(jsonPath("$.applicationUrl").value("https://jobicy.com/jobs/java-backend-developer-intern"))
                .andReturn();
        long importedJobId = objectMapper.readTree(importResult.getResponse().getContentAsString())
                .path("id").asLong();

        // Confirm the imported row is stored as an active shared job.
        var storedJob = jobRepository.findById(importedJobId).orElseThrow();
        assertEquals("JOBICY", storedJob.getSource());
        assertEquals("ACTIVE", storedJob.getStatus());
        assertTrue(storedJob.getOwner() == null, "Imported listing should not have a user owner");
        assertTrue(jobRepository.findByStatusAndOwnerIsNullOrderByCreatedAtDesc("ACTIVE").stream()
                .anyMatch(job -> job.getId().equals(importedJobId)),
                "Imported listing should appear in the public-jobs repository query");

        // A provider listing is public/shared; candidate-specific tracking remains private.
        MvcResult visibleJobs = mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", bearer(candidateToken)))
                .andExpect(status().isOk())
                .andReturn();
        boolean importedListingVisible = false;
        for (JsonNode visibleJob : objectMapper.readTree(visibleJobs.getResponse().getContentAsString())) {
            if (visibleJob.path("id").asLong() == importedJobId) {
                importedListingVisible = true;
                break;
            }
        }
        assertTrue(importedListingVisible,
                "Imported Jobicy listing " + importedJobId + " not present in: "
                        + visibleJobs.getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jobId", importedJobId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobTitle").value("Java Backend Developer Intern"));

        // Importing the same provider ID is idempotent; it does not create duplicates.
        MvcResult duplicateImport = mockMvc.perform(post("/api/v1/jobs/import")
                        .header("Authorization", bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andReturn();
        assertEquals(importedJobId, objectMapper.readTree(duplicateImport.getResponse().getContentAsString())
                .path("id").asLong());

        String unsafeImport = objectMapper.writeValueAsString(Map.of(
                "externalJobId", String.valueOf(System.currentTimeMillis() + 1),
                "companyName", "Fake Company",
                "jobTitle", "Java Developer",
                "excerpt", "Not a trusted source.",
                "location", "Remote",
                "jobType", "Full-Time",
                "experienceLevel", "Entry-level",
                "sourceUrl", "https://example.com/jobs/java"
        ));
        mockMvc.perform(post("/api/v1/jobs/import")
                        .header("Authorization", bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unsafeImport))
                .andExpect(status().isBadRequest());

        // The importing account must not receive the other candidate's application record.
        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String register(String email, String fullName) throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "fullName", fullName,
                "email", email,
                "password", "StrongPassword123!"
        ));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token").asText();
    }

    private byte[] createResumeDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Jahnavi Gajjala");
            document.createParagraph().createRun().setText("Education");
            document.createParagraph().createRun().setText("Skills: Java Spring Boot REST APIs MySQL");
            document.createParagraph().createRun().setText("Projects");
            document.createParagraph().createRun().setText("Experience building backend applications");
            document.write(output);
            return output.toByteArray();
        }
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
