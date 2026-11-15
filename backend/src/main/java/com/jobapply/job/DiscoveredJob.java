package com.jobapply.job;

public record DiscoveredJob(
        String id,
        String title,
        String companyName,
        String location,
        String jobType,
        String experienceLevel,
        String excerpt,
        String sourceUrl,
        String publishedAt,
        String salary
) {}
