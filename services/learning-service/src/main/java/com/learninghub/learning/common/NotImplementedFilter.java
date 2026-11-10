package com.learninghub.learning.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Stub build: every API of learning-service answers 501 Not Implemented until Jay Bruhn Schraml implements it,
 * except the endpoints listed in {@link #IMPLEMENTED}, which reach their controller when the caller has the
 * admin role (teachers and students still get 501 for them) and answer with the header {@code X-Implemented-By: <service> (<owner>)} so the page can show whose work it is.
 * <p>
 * Runs after Spring Security (plain {@code @Component} filter), so unauthenticated calls still get 401 and the
 * wrong role still gets 403; the OpenAPI document and Swagger UI stay reachable so the contract can be read.
 * <p>
 * To implement an endpoint: make its controller method work, then add "METHOD /path" to {@link #IMPLEMENTED}
 * (the path is matched exactly, query string ignored). Delete this class when every endpoint is implemented.
 */
@Component
public class NotImplementedFilter extends OncePerRequestFilter {
    static final String SERVICE = "learning-service";
    static final String OWNER = "Jay Bruhn Schraml";

    /** Endpoints that are implemented: the request goes through to the controller when the caller is an admin. */
    static final Set<String> IMPLEMENTED = Set.of(
            "GET /api/learning/classes"                   // the worked example: lesson-plan summary per visible course (Learning page)
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        return !p.startsWith("/api/") || p.contains("/v3/api-docs") || p.contains("/swagger-ui") || p.contains("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String endpoint = req.getMethod() + " " + req.getRequestURI();
        boolean implemented = IMPLEMENTED.contains(endpoint);
        if (implemented && isAdmin()) {
            res.setHeader("X-Implemented-By", SERVICE + " (" + OWNER + ")");
            chain.doFilter(req, res);
            return;
        }
        String note = implemented ? " — implemented for the admin role only" : "";
        res.setStatus(501);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":501,\"error\":\"Not Implemented\",\"service\":\"" + SERVICE + "\",\"owner\":\"" + OWNER
                + "\",\"endpoint\":\"" + endpoint.replace("\"", "") + "\",\"message\":\"Not Implemented: " + endpoint.replace("\"", "") + " — " + SERVICE + " (" + OWNER + ")" + note + "\"}");
    }

    /** True when the JWT carries the realm role admin (mapped to ROLE_admin by SecurityConfig). */
    private static boolean isAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(g -> "ROLE_admin".equals(g.getAuthority()));
    }
}
