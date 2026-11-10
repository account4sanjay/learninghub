package com.learninghub.payment.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.learninghub.payment.common.CurrentUser;
import com.learninghub.payment.common.Fmt;
import com.learninghub.payment.common.NotFoundException;
import com.learninghub.payment.common.TermBilling;
import com.learninghub.payment.domain.Invoice;
import com.learninghub.payment.domain.InvoiceLine;
import com.learninghub.payment.domain.PaymentMethod;
import com.learninghub.payment.repo.InvoiceLineRepository;
import com.learninghub.payment.repo.InvoiceRepository;
import com.learninghub.payment.repo.PaymentMethodRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Invoice details for the "View Details" / receipt workflow on the Payments page. */
@Tag(name = "Invoice details", description = "One invoice with status, tuition breakdown, payment method, receipt and timeline")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/invoices")
public class InvoiceDetailsController {

    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;
    private final PaymentMethodRepository methods;
    private final TermBilling termBilling;

    @Operation(summary = "Invoice details", description = "The invoice as shown by View Details / Receipt: computed status (Paid, Overdue, Upcoming, Scheduled), "
            + "tuition breakdown from the invoice's own lines (one per billed course, frozen at billing time; courseId references enrollment-service), payment method and date, "
            + "receipt number, timeline and whether it can be paid now. Owner or admin only.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {"id":10,"description":"Q2 · Winter 2026 Tuition","termId":"q2-2026","term":"Q2 · Winter 2026","amount":"$1,600.00","amountValue":1600.00,"dueDate":"Oct 26, 2026","issuedOn":"Sep 26, 2026",
             "status":"Upcoming","daysUntilDue":30,"paidOn":null,"method":null,"receiptNumber":null,"canPay":true,
             "breakdown":[{"courseId":"ap-precalc","subject":"AP Pre Calculus","gradeLevel":null,"credits":null,"amount":"$320.00"},{"courseId":"ap-chemistry","subject":"AP Chemistry","gradeLevel":null,"credits":null,"amount":"$320.00"}],
             "breakdownTotal":"$1,600.00","timeline":[{"label":"Invoice issued","date":"Sep 26, 2026","done":true},{"label":"Due","date":"Oct 26, 2026","done":false},{"label":"Paid","date":null,"done":false}]}
            """)))
    @ApiResponse(responseCode = "403", description = "Not the owner (and not an admin)")
    @ApiResponse(responseCode = "404", description = "Unknown invoice")
    @GetMapping("/{id}/details")
    public Map<String, Object> details(@PathVariable Long id) {
        Invoice inv = invoices.findById(id).orElseThrow(() -> new NotFoundException("invoice", id));
        boolean owner = inv.getUsername().equals(CurrentUser.username());
        if (!owner && !CurrentUser.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your invoice");

        LocalDate today = LocalDate.now();
        boolean paid = "Paid".equals(inv.getStatus());
        String status = paid ? "Paid" : inv.getDueDate().isBefore(today) ? "Overdue" : inv.getStatus();
        LocalDate issued = inv.getDueDate().minusDays(30);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", inv.getId());
        out.put("description", inv.getDescription());
        out.put("termId", inv.getTermId());
        String termName = termBilling.termName(inv.getTermId());
        out.put("term", termName != null ? termName : inv.getDescription().replaceAll("\\s*Tuition$", ""));
        out.put("amount", Fmt.money(inv.getAmount()));
        out.put("amountValue", inv.getAmount());
        out.put("dueDate", Fmt.date(inv.getDueDate()));
        out.put("issuedOn", Fmt.date(issued));
        out.put("status", status);
        out.put("daysUntilDue", paid ? null : ChronoUnit.DAYS.between(today, inv.getDueDate()));
        out.put("paidOn", Fmt.date(inv.getPaidOn()));
        Map<String, Object> method = null;
        if (inv.getMethodId() != null) {
            PaymentMethod m = methods.findById(inv.getMethodId()).orElse(null);
            if (m != null) method = Map.of("brand", m.getBrand(), "last4", m.getLast4(), "expires", m.getExpires());
        }
        out.put("method", method);
        out.put("receiptNumber", paid ? String.format("LH-%06d", inv.getId()) : null);
        out.put("canPay", !paid && owner);

        // breakdown: the invoice's own lines (Ask 46) — one per billed course, frozen at billing time
        List<Map<String, Object>> breakdown = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine l : lines.findByInvoiceIdOrderByIdAsc(inv.getId())) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("courseId", l.getCourseId()); line.put("subject", l.getDescription());
            line.put("gradeLevel", null); line.put("credits", null); line.put("amount", Fmt.money(l.getAmount()));
            breakdown.add(line);
            total = total.add(l.getAmount());
        }
        out.put("breakdown", breakdown);
        out.put("breakdownTotal", Fmt.money(total));

        List<Map<String, Object>> timeline = new ArrayList<>();
        timeline.add(step("Invoice issued", Fmt.date(issued), true));
        timeline.add(step("Due", Fmt.date(inv.getDueDate()), paid || !inv.getDueDate().isAfter(today)));
        timeline.add(step("Paid", Fmt.date(inv.getPaidOn()), paid));
        out.put("timeline", timeline);
        return out;
    }

    private static Map<String, Object> step(String label, String date, boolean done) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label); m.put("date", date); m.put("done", done);
        return m;
    }
}
