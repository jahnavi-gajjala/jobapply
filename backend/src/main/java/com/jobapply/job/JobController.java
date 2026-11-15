package com.jobapply.job;

import com.jobapply.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobRepository jobs;
    private final UserRepository users;

    public JobController(JobRepository jobs, UserRepository users) {
        this.jobs = jobs;
        this.users = users;
    }

    record JobRequest(
            @NotBlank @Size(max = 150) String companyName,
            @NotBlank @Size(max = 200) String jobTitle,
            @Size(max = 20000) String jobDescription,
            @Size(max = 150) String location,
            @Size(max = 50) String jobType,
            @Size(max = 50) String experienceLevel,
            Integer graduationYear,
            @Size(max = 1000) String applicationUrl
    ) {}

    record ImportJobRequest(
            @NotBlank @Pattern(regexp = "\\d{1,30}") String externalJobId,
            @NotBlank @Size(max = 150) String companyName,
            @NotBlank @Size(max = 200) String jobTitle,
            @Size(max = 1600) String excerpt,
            @Size(max = 150) String location,
            @Size(max = 150) String jobType,
            @Size(max = 100) String experienceLevel,
            @NotBlank @Size(max = 1000) String sourceUrl
    ) {}

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String q,
            Authentication auth
    ) {
        List<Job> all = auth == null
                ? jobs.findByStatusAndOwnerIsNullOrderByCreatedAtDesc("ACTIVE")
                : jobs.findVisibleToUser(auth.getName());

        if (q != null && !q.isBlank()) {
            String term = q.trim().toLowerCase();
            all = all.stream()
                    .filter(job -> job.getJobTitle().toLowerCase().contains(term)
                            || job.getCompanyName().toLowerCase().contains(term)
                            || (job.getLocation() != null && job.getLocation().toLowerCase().contains(term)))
                    .toList();
        }
        return all.stream().map(this::view).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id, Authentication auth) {
        Job job = auth == null
                ? jobs.findPublicById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
                : jobs.findVisibleById(id, auth.getName())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return view(job);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody JobRequest request, Authentication auth) {
        Job job = new Job();
        job.setOwner(users.findByEmail(auth.getName()).orElseThrow());
        job.setCompanyName(request.companyName().trim());
        job.setJobTitle(request.jobTitle().trim());
        job.setJobDescription(request.jobDescription());
        job.setLocation(request.location());
        if (request.jobType() != null) job.setJobType(request.jobType());
        if (request.experienceLevel() != null) job.setExperienceLevel(request.experienceLevel());
        job.setGraduationYear(request.graduationYear());
        job.setApplicationUrl(request.applicationUrl());
        job.setSource("USER_ADDED");
        return view(jobs.save(job));
    }

    /**
     * Converts a discovered public Jobicy listing into a shared local job record.
     * The source URL is restricted to canonical Jobicy listing URLs so an arbitrary
     * client cannot turn the source link into an open redirect or phishing link.
     */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> importJob(@Valid @RequestBody ImportJobRequest request) {
        String externalId = request.externalJobId().trim();
        return jobs.findBySourceAndExternalJobId("JOBICY", externalId)
                .map(this::view)
                .orElseGet(() -> createImportedJob(request, externalId));
    }

    private Map<String, Object> createImportedJob(ImportJobRequest request, String externalId) {
        if (!isCanonicalJobicyUrl(request.sourceUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid Jobicy listing URL is required");
        }

        Job job = new Job();
        job.setOwner(null); // External listings are shared; user-specific tracking stays in applications.
        job.setSource("JOBICY");
        job.setExternalJobId(externalId);
        job.setCompanyName(request.companyName().trim());
        job.setJobTitle(request.jobTitle().trim());
        job.setJobDescription(request.excerpt() == null ? "" : request.excerpt().trim());
        job.setLocation(request.location() == null ? "" : request.location().trim());
        job.setJobType(request.jobType() == null || request.jobType().isBlank()
                ? "REMOTE"
                : request.jobType().trim());
        job.setExperienceLevel(request.experienceLevel() == null
                ? ""
                : request.experienceLevel().trim());
        job.setApplicationUrl(request.sourceUrl());
        job.setStatus("ACTIVE");

        try {
            return view(jobs.saveAndFlush(job));
        } catch (DataIntegrityViolationException exception) {
            // Another account may have imported the same listing at the same time.
            return jobs.findBySourceAndExternalJobId("JOBICY", externalId)
                    .map(this::view)
                    .orElseThrow(() -> exception);
        }
    }

    private boolean isCanonicalJobicyUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && (host.equalsIgnoreCase("jobicy.com") || host.equalsIgnoreCase("www.jobicy.com"))
                    && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && uri.getPath() != null
                    && uri.getPath().startsWith("/jobs/");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private Map<String, Object> view(Job job) {
        return Map.ofEntries(
                Map.entry("id", job.getId()),
                Map.entry("companyName", job.getCompanyName()),
                Map.entry("jobTitle", job.getJobTitle()),
                Map.entry("jobDescription", job.getJobDescription() == null ? "" : job.getJobDescription()),
                Map.entry("location", job.getLocation() == null ? "" : job.getLocation()),
                Map.entry("jobType", job.getJobType() == null ? "" : job.getJobType()),
                Map.entry("experienceLevel", job.getExperienceLevel() == null ? "" : job.getExperienceLevel()),
                Map.entry("graduationYear", job.getGraduationYear() == null ? 0 : job.getGraduationYear()),
                Map.entry("applicationUrl", job.getApplicationUrl() == null ? "" : job.getApplicationUrl()),
                Map.entry("source", job.getSource()),
                Map.entry("externalJobId", job.getExternalJobId() == null ? "" : job.getExternalJobId())
        );
    }
}
