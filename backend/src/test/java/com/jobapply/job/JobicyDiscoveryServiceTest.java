package com.jobapply.job;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class JobicyDiscoveryServiceTest {

    @Test
    void mapsRemoteJobsAndPreservesTheProviderCursor() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JobicyDiscoveryService service = new JobicyDiscoveryService(builder.build());

        String payload = """
                {
                  "success": true,
                  "lastUpdate": "2026-11-15T09:35:37+00:00",
                  "nextCursor": "opaque-next-cursor",
                  "hasMore": true,
                  "jobs": [{
                    "id": 123456,
                    "url": "https://jobicy.com/jobs/java-backend-developer",
                    "jobTitle": "Java Backend Developer",
                    "companyName": "Northstar Labs",
                    "jobType": ["Full-Time", "Remote"],
                    "jobGeo": "APAC",
                    "jobLevel": "Entry-level",
                    "jobExcerpt": "<p>Build Java REST APIs &amp; backend services.</p>",
                    "pubDate": "2026-11-15T08:00:00+00:00",
                    "salaryMin": 60000,
                    "salaryMax": 80000,
                    "salaryCurrency": "USD",
                    "salaryPeriod": "yearly"
                  }]
                }
                """;

        server.expect(requestTo("https://jobicy.com/api/jobs?count=20&geo=apac&tag=Java"))
                .andExpect(method(GET))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        JobDiscoveryResponse response = service.search(" Java ", "APAC", 20, null);

        assertEquals(1, response.jobs().size());
        DiscoveredJob job = response.jobs().get(0);
        assertEquals("123456", job.id());
        assertEquals("Java Backend Developer", job.title());
        assertEquals("Northstar Labs", job.companyName());
        assertEquals("Build Java REST APIs & backend services.", job.excerpt());
        assertEquals("Full-Time, Remote", job.jobType());
        assertEquals("USD 60000–80000 / yearly", job.salary());
        assertEquals("opaque-next-cursor", response.nextCursor());
        assertTrue(response.hasMore());
        assertEquals("Jobicy", response.source());
        server.verify();
    }

    @Test
    void dropsListingsWithNonCanonicalProviderUrls() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JobicyDiscoveryService service = new JobicyDiscoveryService(builder.build());

        String payload = """
                {
                  "success": true,
                  "nextCursor": null,
                  "hasMore": false,
                  "jobs": [{
                    "id": 123456,
                    "url": "https://example.com/phishing",
                    "jobTitle": "Java Developer",
                    "companyName": "Example Labs",
                    "jobExcerpt": "A role."
                  }]
                }
                """;

        server.expect(requestTo("https://jobicy.com/api/jobs?count=20&geo=apac&tag=Java"))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        JobDiscoveryResponse response = service.search("Java", "apac", 20, null);

        assertTrue(response.jobs().isEmpty());
        assertTrue(!response.hasMore());
        server.verify();
    }
}
