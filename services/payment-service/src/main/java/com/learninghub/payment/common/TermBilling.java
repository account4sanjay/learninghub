package com.learninghub.payment.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.learninghub.payment.domain.Invoice;
import com.learninghub.payment.domain.InvoiceLine;
import com.learninghub.payment.repo.InvoiceLineRepository;
import com.learninghub.payment.repo.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ask 46 — terms are owned by schedule-service and courses by enrollment-service; payment-service only stores their ids.
 * <ul>
 *   <li>{@link #terms()} reads GET /api/schedule/terms (caller's token forwarded) and caches the list for 60 s.</li>
 *   <li>{@link #ensureCurrentTermInvoice(String)} is the lazy term billing: a student with no invoice for the current term
 *       gets one, with one line per enrolled course ($320 each) read from enrollment-service /my-classes. Lines are frozen
 *       at billing time; later drops are not credited. payment-service stays the single writer of its own tables.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TermBilling {
    public static final BigDecimal PER_SUBJECT = new BigDecimal("320.00");
    private static final long TTL_MS = 60_000;
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> OBJ = new ParameterizedTypeReference<>() {};

    private final ServiceClient client;
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;

    private volatile List<Map<String, Object>> cachedTerms = List.of();
    private volatile long cachedAt;

    /** All terms from schedule-service ({id,name,start,end,current,...}); empty when unavailable. */
    public List<Map<String, Object>> terms() {
        long now = System.currentTimeMillis();
        if (!cachedTerms.isEmpty() && now - cachedAt < TTL_MS) return cachedTerms;
        List<Map<String, Object>> t = client.get("schedule-service", "/api/schedule/terms", LIST, List.of());
        if (!t.isEmpty()) { cachedTerms = t; cachedAt = now; }
        return t;
    }

    /** Term name for an id; null when unknown. */
    public String termName(String termId) {
        if (termId == null) return null;
        for (Map<String, Object> t : terms()) if (termId.equals(String.valueOf(t.get("id")))) return (String) t.get("name");
        return null;
    }

    private Map<String, Object> currentTerm() {
        for (Map<String, Object> t : terms()) if (Boolean.TRUE.equals(t.get("current"))) return t;
        return null;
    }

    /** Create the caller's invoice for the current term when none exists yet (students only). */
    @Transactional
    public void ensureCurrentTermInvoice(String username) {
        if (!CurrentUser.isStudent()) return;
        Map<String, Object> term = currentTerm();
        if (term == null || term.get("id") == null) return;
        String termId = String.valueOf(term.get("id"));
        if (invoices.findFirstByUsernameAndTermId(username, termId).isPresent()) return;
        Map<String, Object> mine = client.get("enrollment-service", "/api/enrollment/my-classes", OBJ, Map.of());
        if (!(mine.get("classes") instanceof List<?> classes) || classes.isEmpty()) return;
        List<InvoiceLine> newLines = new ArrayList<>();
        for (Object c : classes) {
            if (!(c instanceof Map<?, ?> m) || m.get("id") == null) continue;
            InvoiceLine l = new InvoiceLine();
            l.setCourseId(String.valueOf(m.get("id")));
            l.setDescription(m.get("name") == null ? String.valueOf(m.get("id")) : String.valueOf(m.get("name")));
            l.setAmount(PER_SUBJECT);
            newLines.add(l);
        }
        if (newLines.isEmpty()) return;
        LocalDate due;
        try { due = LocalDate.parse(String.valueOf(term.get("start"))); } catch (Exception e) { due = LocalDate.now().plusDays(30); }
        Invoice inv = new Invoice();
        inv.setUsername(username); inv.setTermId(termId);
        inv.setDescription(term.get("name") + " Tuition");
        inv.setAmount(PER_SUBJECT.multiply(BigDecimal.valueOf(newLines.size())));
        inv.setDueDate(due); inv.setStatus(Invoice.UPCOMING);
        inv = invoices.save(inv);
        for (InvoiceLine l : newLines) l.setInvoiceId(inv.getId());
        lines.saveAll(newLines);
        log.info("term billing: invoice {} for {} · {} · {} lines", inv.getId(), username, termId, newLines.size());
    }
}
