package com.jobapply.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

@Service
public class JobicyDiscoveryService {
    private static final String SOURCE_NOTICE =
            "Job listings provided by Jobicy. Open the original listing for full details and application instructions.";
    private final RestClient client;

    @Autowired
    public JobicyDiscoveryService(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(12));
        this.client = builder
                .baseUrl("https://jobicy.com")
                .requestFactory(requestFactory)
                .build();
    }

    JobicyDiscoveryService(RestClient client) {
        this.client = client;
    }

    public JobDiscoveryResponse search(String keyword, String geo, int count, String cursor) {
        String normalizedGeo = normalizeGeo(geo);
        int safeCount = Math.max(1, Math.min(count, 50));
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!normalizedKeyword.isBlank()
                && (normalizedKeyword.length() < 3 || normalizedKeyword.length() > 50)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Keyword must be between 3 and 50 characters, or left blank"
            );
        }
        if (cursor != null && cursor.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination cursor");
        }

        try {
            var requestUri = UriComponentsBuilder.fromUriString("https://jobicy.com/api/jobs")
                    .queryParam("count", safeCount)
                    .queryParam("geo", normalizedGeo);
            if (!normalizedKeyword.isBlank()) {
                requestUri.queryParam("tag", normalizedKeyword);
            }
            if (cursor != null && !cursor.isBlank()) {
                requestUri.queryParam("cursor", cursor);
            }

            JsonNode response = client.get()
                    .uri(requestUri.build().encode().toUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("success").asBoolean(true) == false) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Jobicy returned an invalid response. Please try again."
                );
            }

            List<DiscoveredJob> jobs = new ArrayList<>();
            JsonNode items = response.path("jobs");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    DiscoveredJob job = mapJob(item);
                    if (job != null) {
                        jobs.add(job);
                    }
                }
            }

            String nextCursor = response.path("nextCursor").isTextual()
                    ? response.path("nextCursor").asText()
                    : null;
            String lastUpdate = response.path("lastUpdate").asText("");
            return new JobDiscoveryResponse(
                    List.copyOf(jobs),
                    nextCursor,
                    response.path("hasMore").asBoolean(nextCursor != null),
                    "Jobicy",
                    SOURCE_NOTICE,
                    lastUpdate
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Live job search is temporarily unavailable. Please try again shortly.",
                    exception
            );
        }
    }

    private String normalizeGeo(String geo) {
        String value = geo == null || geo.isBlank()
                ? "apac"
                : geo.trim().toLowerCase(Locale.ROOT);
        // These are the two regional options surfaced in the initial JobApply UI.
        if (!value.equals("apac") && !value.equals("anywhere")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Location must be APAC or Anywhere"
            );
        }
        return value;
    }

    private DiscoveredJob mapJob(JsonNode item) {
        String id = item.path("id").asText("");
        String title = cleanText(item.path("jobTitle").asText(""));
        String company = cleanText(item.path("companyName").asText(""));
        String sourceUrl = item.path("url").asText("");
        if (id.isBlank() || title.isBlank() || company.isBlank() || !isJobicyListingUrl(sourceUrl)) {
            return null;
        }

        String excerpt = cleanText(item.path("jobExcerpt").asText(""));
        if (excerpt.length() > 1600) {
            excerpt = excerpt.substring(0, 1600);
        }

        return new DiscoveredJob(
                id,
                title,
                company,
                cleanText(item.path("jobGeo").asText("")),
                joinArray(item.path("jobType")),
                cleanText(item.path("jobLevel").asText("")),
                excerpt,
                sourceUrl,
                item.path("pubDate").asText(""),
                formatSalary(item)
        );
    }

    private boolean isJobicyListingUrl(String value) {
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

    private String joinArray(JsonNode values) {
        if (!values.isArray()) {
            return cleanText(values.asText(""));
        }
        StringJoiner result = new StringJoiner(", ");
        values.forEach(value -> {
            String item = cleanText(value.asText(""));
            if (!item.isBlank()) {
                result.add(item);
            }
        });
        return result.toString();
    }

    private String formatSalary(JsonNode item) {
        if (!item.hasNonNull("salaryMin") && !item.hasNonNull("salaryMax")) {
            return "";
        }
        String currency = item.path("salaryCurrency").asText("");
        String period = item.path("salaryPeriod").asText("");
        String min = item.path("salaryMin").asText("");
        String max = item.path("salaryMax").asText("");
        String range;
        if (!min.isBlank() && !max.isBlank()) {
            range = min + "–" + max;
        } else {
            range = min.isBlank() ? max : min;
        }
        return (currency.isBlank() ? "" : currency + " ")
                + range
                + (period.isBlank() ? "" : " / " + period);
    }

    private String cleanText(String value) {
        return HtmlUtils.htmlUnescape(value == null ? "" : value)
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
