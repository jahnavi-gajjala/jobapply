package com.jobapply.application;

import com.jobapply.job.Job;
import com.jobapply.job.JobRepository;
import com.jobapply.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {
    private static final String EMAIL = "candidate@example.com";

    @Mock private ApplicationRepository applications;
    @Mock private JobRepository jobs;
    @Mock private UserRepository users;
    @Mock private ApplicationActivityRepository activities;

    private ApplicationController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new ApplicationController(applications, jobs, users, activities);
        authentication = new UsernamePasswordAuthenticationToken(EMAIL, "n/a", List.of());
    }

    @Test
    void changingStatusToSubmittedRecordsSubmissionTimeAndActivity() {
        ApplicationRecord application = mock(ApplicationRecord.class);
        Job job = mock(Job.class);

        when(applications.findByIdAndUserEmail(11L, EMAIL)).thenReturn(Optional.of(application));
        when(applications.save(application)).thenReturn(application);
        when(application.getStatus()).thenReturn("SAVED", "SUBMITTED");
        when(application.getSubmittedAt()).thenReturn(null, Instant.parse("2026-11-15T10:00:00Z"));
        when(application.getId()).thenReturn(11L);
        when(application.getJob()).thenReturn(job);
        when(application.getApplicationMode()).thenReturn("MANUAL");
        when(application.getNotes()).thenReturn(null);
        when(application.getInterviewDate()).thenReturn(null);
        when(application.getFollowUpDate()).thenReturn(null);
        when(application.getCreatedAt()).thenReturn(Instant.parse("2026-11-01T10:00:00Z"));
        when(job.getId()).thenReturn(21L);
        when(job.getCompanyName()).thenReturn("Example Company");
        when(job.getJobTitle()).thenReturn("Java Developer Intern");
        when(job.getLocation()).thenReturn("Remote");

        Map<String, Object> response = controller.status(
                11L,
                new ApplicationController.StatusRequest("submitted"),
                authentication
        );

        assertEquals("SUBMITTED", response.get("status"));
        assertEquals(11L, response.get("id"));
        assertEquals(21L, response.get("jobId"));
        assertEquals("Example Company", response.get("companyName"));
        assertNotNull(response.get("submittedAt"));
        verify(application).setStatus("SUBMITTED");
        verify(application).setSubmittedAt(any(Instant.class));

        ArgumentCaptor<ApplicationActivity> activityCaptor =
                ArgumentCaptor.forClass(ApplicationActivity.class);
        verify(activities).save(activityCaptor.capture());
        assertEquals("STATUS_CHANGED", activityCaptor.getValue().getEventType());
        assertEquals("SAVED", activityCaptor.getValue().getOldStatus());
        assertEquals("SUBMITTED", activityCaptor.getValue().getNewStatus());
    }

    @Test
    void rejectsUnsupportedApplicationStatusBeforeLoadingRecord() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.status(
                        11L,
                        new ApplicationController.StatusRequest("NOT_A_STATUS"),
                        authentication
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(applications, activities);
    }
}
