package com.learninghub.assessment.common;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ask 46: terms are owned by schedule-service (GET /api/schedule/terms). The current term is cached for
 * 60 s; when schedule-service cannot be reached and nothing is cached the request fails with 503.
 */
@Component
@RequiredArgsConstructor
public class Terms {
    private static final long TTL_MS = 60_000;
    private final ServiceClient client;
    private volatile Term cached;
    private volatile long cachedAt;

    public record Term(String id, String name, LocalDate start, LocalDate end, boolean current, int maxCredits) {}

    /** The current term (name, dates) from schedule-service. */
    public Term current() {
        Term t = cached;
        if (t != null && System.currentTimeMillis() - cachedAt < TTL_MS) return t;
        List<Map<String, Object>> rows = client.get("schedule-service", "/api/schedule/terms",
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}, null);
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                if (!Boolean.TRUE.equals(r.get("current")) && !"true".equals(String.valueOf(r.get("current")))) continue;
                t = new Term(String.valueOf(r.get("id")), String.valueOf(r.get("name")),
                        LocalDate.parse(String.valueOf(r.get("start"))), LocalDate.parse(String.valueOf(r.get("end"))), true,
                        r.get("maxCredits") instanceof Number n ? n.intValue() : 30);
                cached = t; cachedAt = System.currentTimeMillis();
                return t;
            }
        }
        if (cached != null) return cached;   // stale beats nothing
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "schedule-service (current term) is unavailable");
    }

    /** {name, start, end} as the pages render it. */
    public static Map<String, Object> json(Term t) {
        return Map.of("name", t.name(), "start", Fmt.date(t.start()), "end", Fmt.date(t.end()));
    }
}
