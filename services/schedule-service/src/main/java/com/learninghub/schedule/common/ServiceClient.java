package com.learninghub.schedule.common;

import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the other LearningHub services on behalf of the current user.
 * <ul>
 *   <li>If {@code learninghub.gateway-url} is set (host runs), requests go to {@code <gateway-url>/api/...}.</li>
 *   <li>Otherwise a {@link LoadBalanced} client resolves {@code http://<service-name>/api/...} via Eureka.</li>
 * </ul>
 * The caller's bearer token is always forwarded. Every call has a fallback: the timetable
 * must render even when a downstream service is down or not yet registered.
 */
@Slf4j
@Component
public class ServiceClient {

    private final String gatewayUrl;
    private final RestClient direct;
    private final RestClient loadBalanced;

    public ServiceClient(@Value("${learninghub.gateway-url:}") String gatewayUrl,
                         @LoadBalanced RestClient.Builder lbBuilder) {
        this.gatewayUrl = gatewayUrl == null ? "" : gatewayUrl.replaceAll("/+$", "");
        this.direct = RestClient.builder().build();
        this.loadBalanced = lbBuilder.build();
    }

    /** GET {@code path} (e.g. "/api/schedule/today") on {@code service} (e.g. "schedule-service"). */
    public <T> T get(String service, String path, ParameterizedTypeReference<T> type, T fallback) {
        return guard(service + path, fallback, () -> {
            String url = gatewayUrl.isEmpty() ? "http://" + service + path : gatewayUrl + path;
            RestClient client = gatewayUrl.isEmpty() ? loadBalanced : direct;
            String token = CurrentUser.bearerToken();
            RestClient.RequestHeadersSpec<?> req = client.get().uri(url);
            if (token != null) req = req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return req.retrieve().body(type);
        });
    }

    private <T> T guard(String what, T fallback, Supplier<T> call) {
        try {
            T v = call.get();
            return v == null ? fallback : v;
        } catch (Exception e) {
            log.warn("downstream call {} failed ({}); using fallback", what, e.getMessage());
            return fallback;
        }
    }
}
