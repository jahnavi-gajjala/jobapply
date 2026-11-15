package com.jobapply.job;

import java.util.List;

public record JobDiscoveryResponse(
        List<DiscoveredJob> jobs,
        String nextCursor,
        boolean hasMore,
        String source,
        String sourceNotice,
        String lastUpdate
) {}
