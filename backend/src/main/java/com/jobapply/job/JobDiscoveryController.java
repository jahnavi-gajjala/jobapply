package com.jobapply.job;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobDiscoveryController {
    private final JobicyDiscoveryService discovery;

    public JobDiscoveryController(JobicyDiscoveryService discovery) {
        this.discovery = discovery;
    }

    @GetMapping("/discover")
    public JobDiscoveryResponse discover(
            @RequestParam(required = false, defaultValue = "Java") String keyword,
            @RequestParam(required = false, defaultValue = "apac") String geo,
            @RequestParam(required = false, defaultValue = "20") int count,
            @RequestParam(required = false) String cursor
    ) {
        return discovery.search(keyword, geo, count, cursor);
    }
}
