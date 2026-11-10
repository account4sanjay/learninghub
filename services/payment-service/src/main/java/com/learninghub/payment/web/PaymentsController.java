package com.learninghub.payment.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.learninghub.payment.common.CurrentUser;
import com.learninghub.payment.common.Fmt;
import com.learninghub.payment.common.NotFoundException;
import com.learninghub.payment.domain.BillingProfile;
import com.learninghub.payment.domain.Invoice;
import com.learninghub.payment.domain.PaymentMethod;
import com.learninghub.payment.repo.BillingProfileRepository;
import com.learninghub.payment.repo.InvoiceRepository;
import com.learninghub.payment.repo.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * View endpoints for the Payments page, computed from the caller's invoice,
 * payment_method and billing_profile rows.
 */
@Tag(name = "Payments", description = "Term tuition invoices, cards and billing profile of the caller; admins get the school-wide view with a username on every row.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentsController {

    private final InvoiceRepository invoices;
    private final PaymentMethodRepository methods;
    private final BillingProfileRepository billing;
    private final com.learninghub.payment.common.TermBilling termBilling;

    private static Map<String, Object> stat(String label, String value, String sub, String color, String icon) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label); m.put("value", value); m.put("sub", sub); m.put("color", color); m.put("icon", icon);
        return m;
    }

    /** Admin: school-wide totals over every student's invoices. */
    private Map<String, Object> adminSummary() {
        LocalDate today = LocalDate.now();
        List<Invoice> paid = invoices.findByStatusOrderByPaidOnDescIdDesc(Invoice.PAID);
        List<Invoice> unpaid = invoices.findByStatusNotOrderByDueDateAscIdAsc(Invoice.PAID);
        BigDecimal collected = paid.stream().map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = unpaid.stream().map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Invoice> overdue = unpaid.stream().filter(i -> i.getDueDate().isBefore(today)).toList();
        BigDecimal overdueSum = overdue.stream().map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long students = invoices.findAll().stream().map(Invoice::getUsername).distinct().count();
        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(stat("Total Collected", Fmt.money(collected), paid.size() + " payment" + (paid.size() == 1 ? "" : "s"), "blue", "paid"));
        stats.add(stat("Outstanding", Fmt.money(outstanding), unpaid.size() + " open invoice" + (unpaid.size() == 1 ? "" : "s"), "purple", "upcoming"));
        stats.add(stat("Overdue", Fmt.money(overdueSum), overdue.isEmpty() ? "Nothing overdue" : overdue.size() + " overdue", "amber", "pending"));
        stats.add(stat("Students Billed", String.valueOf(students), "With at least one invoice", "green", "due"));
        return Map.of("stats", stats, "scope", "school");
    }

    @Operation(summary = "Payment summary", description = "Student: Total Paid / Upcoming / Pending / Amount Due (a student with no invoice for the current term is billed first from their enrolled courses). Admin: Total Collected / Outstanding / Overdue / Students Billed across all users.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "student", value = "{\"stats\":[{\"label\":\"Total Paid\",\"value\":\"$1,600.00\",\"sub\":\"1 payment\",\"color\":\"blue\",\"icon\":\"paid\"},{\"label\":\"Upcoming Payment\",\"value\":\"$1,600.00\",\"sub\":\"Due Oct 26, 2026\",\"color\":\"purple\",\"icon\":\"upcoming\"},{\"label\":\"Pending\",\"value\":\"$0.00\",\"sub\":\"No pending payments\",\"color\":\"amber\",\"icon\":\"pending\"},{\"label\":\"Amount Due\",\"value\":\"$0.00\",\"sub\":\"All up to date\",\"color\":\"green\",\"icon\":\"due\"}]}"), @ExampleObject(name = "admin (school-wide)", value = "{\"stats\":[{\"label\":\"Total Collected\",\"value\":\"$35,520.00\",\"sub\":\"29 payments\",\"color\":\"blue\",\"icon\":\"paid\"},{\"label\":\"Outstanding\",\"value\":\"$66,880.00\",\"sub\":\"55 open invoices\",\"color\":\"purple\",\"icon\":\"upcoming\"},{\"label\":\"Overdue\",\"value\":\"$4,800.00\",\"sub\":\"4 invoices\",\"color\":\"amber\",\"icon\":\"pending\"},{\"label\":\"Students Billed\",\"value\":\"21\",\"sub\":\"\",\"color\":\"green\",\"icon\":\"due\"}],\"scope\":\"school\"}")}))
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        if (CurrentUser.isAdmin()) return adminSummary();
        String user = CurrentUser.username();
        termBilling.ensureCurrentTermInvoice(user);   // Ask 46: lazy term billing
        LocalDate today = LocalDate.now();
        List<Invoice> paid = invoices.findByUsernameAndStatusOrderByPaidOnDescIdDesc(user, Invoice.PAID);
        List<Invoice> unpaid = invoices.findByUsernameAndStatusNotOrderByDueDateAscIdAsc(user, Invoice.PAID);

        BigDecimal totalPaid = paid.stream().map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Optional<Invoice> next = unpaid.stream().filter(i -> !i.getDueDate().isBefore(today)).findFirst();
        BigDecimal pending = unpaid.stream().filter(i -> Invoice.PENDING.equals(i.getStatus()))
                .map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Invoice> overdue = unpaid.stream().filter(i -> i.getDueDate().isBefore(today)).toList();
        BigDecimal due = overdue.stream().map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(stat("Total Paid", Fmt.money(totalPaid),
                paid.size() + " payment" + (paid.size() == 1 ? "" : "s"), "blue", "paid"));
        stats.add(stat("Upcoming Payment", next.map(i -> Fmt.money(i.getAmount())).orElse("$0.00"),
                next.map(i -> "Due " + Fmt.date(i.getDueDate())).orElse("Nothing scheduled"), "purple", "upcoming"));
        stats.add(stat("Pending", Fmt.money(pending),
                pending.signum() == 0 ? "No pending payments" : "Awaiting confirmation", "amber", "pending"));
        stats.add(stat("Amount Due", Fmt.money(due),
                overdue.isEmpty() ? "All up to date" : overdue.size() + " overdue", "green", "due"));
        return Map.of("stats", stats);
    }

    @Operation(summary = "Unpaid invoices", description = "Unpaid invoices by due date (a student with no invoice for the current term is billed first: one line per enrolled course, $320 each); the first one gets action 'Pay Now'. Admin rows carry username.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"id\":94,\"dueDate\":\"Oct 26, 2026\",\"description\":\"Q2 · Winter 2026 Tuition\",\"termId\":\"q2-2026\",\"amount\":\"$1,600.00\",\"status\":\"Upcoming\",\"action\":\"Pay Now\"},{\"id\":95,\"dueDate\":\"Jan 19, 2027\",\"description\":\"Q3 · Winter 2027 Tuition\",\"termId\":\"q3-2027\",\"amount\":\"$1,600.00\",\"status\":\"Scheduled\",\"action\":\"View Details\"}]")}))
    @GetMapping("/upcoming")
    public List<Map<String, Object>> upcoming() {
        List<Map<String, Object>> out = new ArrayList<>();
        boolean admin = CurrentUser.isAdmin();
        if (!admin) termBilling.ensureCurrentTermInvoice(CurrentUser.username());   // Ask 46: lazy term billing
        boolean first = true;
        List<Invoice> rows = admin ? invoices.findByStatusNotOrderByDueDateAscIdAsc(Invoice.PAID)
                : invoices.findByUsernameAndStatusNotOrderByDueDateAscIdAsc(CurrentUser.username(), Invoice.PAID);
        for (Invoice i : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId()); m.put("dueDate", Fmt.date(i.getDueDate())); m.put("description", i.getDescription()); m.put("termId", i.getTermId());
            m.put("amount", Fmt.money(i.getAmount())); m.put("status", i.getStatus());
            m.put("action", admin ? "View Details" : first ? "Pay Now" : "View Details");
            if (admin) m.put("username", i.getUsername());
            out.add(m);
            first = false;
        }
        return out;
    }

    @Operation(summary = "Paid invoices", description = "Paid invoices, newest first. Admin rows carry username.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"id\":93,\"date\":\"Aug 20, 2026\",\"description\":\"Q1 · Fall 2026 Tuition\",\"termId\":\"q1-2026\",\"amount\":\"$1,600.00\",\"status\":\"Paid\"}]")}))
    @GetMapping("/history")
    public List<Map<String, Object>> history() {
        List<Map<String, Object>> out = new ArrayList<>();
        boolean admin = CurrentUser.isAdmin();
        List<Invoice> rows = admin ? invoices.findByStatusOrderByPaidOnDescIdDesc(Invoice.PAID)
                : invoices.findByUsernameAndStatusOrderByPaidOnDescIdDesc(CurrentUser.username(), Invoice.PAID);
        for (Invoice i : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId()); m.put("date", Fmt.date(i.getPaidOn() != null ? i.getPaidOn() : i.getDueDate()));
            m.put("description", i.getDescription()); m.put("termId", i.getTermId()); m.put("amount", Fmt.money(i.getAmount())); m.put("status", i.getStatus());
            if (admin) m.put("username", i.getUsername());
            out.add(m);
        }
        return out;
    }

    @Operation(summary = "My cards", description = "The caller's payment methods.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"id\":1,\"brand\":\"Visa\",\"last4\":\"4242\",\"expires\":\"08/2028\",\"isDefault\":true},{\"id\":2,\"brand\":\"Mastercard\",\"last4\":\"5678\",\"expires\":\"03/2027\",\"isDefault\":false}]")}))
    @GetMapping("/methods")
    public List<Map<String, Object>> methods() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PaymentMethod p : methods.findByUsernameOrderByIsDefaultDescIdAsc(CurrentUser.username())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId()); m.put("brand", p.getBrand()); m.put("last4", p.getLast4());
            m.put("expires", p.getExpires()); m.put("isDefault", p.isDefault());
            out.add(m);
        }
        return out;
    }

    @Operation(summary = "My billing profile", description = "404 when the caller has no billing profile.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"name\":\"John Smith\",\"email\":\"john.smith@gmail.com\",\"address\":\"1234 Oak Ave, Sunnyvale, CA 94086\"}")}))
    @GetMapping("/billing")
    public Map<String, Object> billing() {
        BillingProfile b = billing.findByUsername(CurrentUser.username())
                .orElseThrow(() -> new NotFoundException("billing profile for", CurrentUser.username()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", b.getName()); m.put("email", b.getEmail()); m.put("address", b.getAddress());
        return m;
    }

    /**
     * Pay an invoice. Body may be an upcoming row ({id,...}), {description,...} or just
     * {amount, method} from the side panel — in that case the next unpaid invoice is paid.
     */
    @Operation(summary = "Pay an invoice", description = "Finds the caller's invoice by id, else by description, else the next unpaid one; marks it Paid today with the given/default card and returns the updated row.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"id\":94,\"description\":\"Q2 · Winter 2026 Tuition\",\"termId\":\"q2-2026\",\"amount\":\"$1,600.00\",\"date\":\"Sep 27, 2026\",\"status\":\"Paid\",\"methodId\":1}")}))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false, content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "by id", value = "{\"id\": 94}"), @ExampleObject(name = "by description", value = "{\"description\": \"Q2 · Winter 2026 Tuition\"}"), @ExampleObject(name = "next unpaid with a card", value = "{\"amount\": \"$1,600.00\", \"methodId\": 1}")}))
    @PostMapping("/pay")
    @Transactional
    public Map<String, Object> pay(@RequestBody(required = false) Map<String, Object> body) {
        String user = CurrentUser.username();
        Map<String, Object> b = body == null ? Map.of() : body;
        Optional<Invoice> target = Optional.empty();
        if (b.get("id") != null) {
            target = invoices.findByIdAndUsername(Long.valueOf(String.valueOf(b.get("id"))), user);
        }
        if (target.isEmpty() && b.get("description") != null) {
            target = invoices.findFirstByUsernameAndDescriptionAndStatusNot(user, String.valueOf(b.get("description")), Invoice.PAID);
        }
        if (target.isEmpty()) {
            target = invoices.findByUsernameAndStatusNotOrderByDueDateAscIdAsc(user, Invoice.PAID).stream().findFirst();
        }
        Invoice inv = target.orElseThrow(() -> new NotFoundException("unpaid invoice for", user));
        if (inv.isPaid()) throw new NotFoundException("unpaid invoice", inv.getId());

        Long methodId = null;
        if (b.get("methodId") != null) methodId = Long.valueOf(String.valueOf(b.get("methodId")));
        if (methodId == null && b.get("method") instanceof String label) {
            // "Visa •••• 4242" from the side panel
            methodId = methods.findByUsernameOrderByIsDefaultDescIdAsc(user).stream()
                    .filter(m -> label.contains(m.getLast4())).map(PaymentMethod::getId).findFirst().orElse(null);
        }
        if (methodId == null) methodId = methods.findFirstByUsernameAndIsDefaultTrue(user).map(PaymentMethod::getId).orElse(null);

        inv.setStatus(Invoice.PAID);
        inv.setPaidOn(LocalDate.now());
        inv.setMethodId(methodId);
        invoices.save(inv);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", inv.getId()); m.put("description", inv.getDescription()); m.put("termId", inv.getTermId()); m.put("amount", Fmt.money(inv.getAmount()));
        m.put("date", Fmt.date(inv.getPaidOn())); m.put("status", inv.getStatus()); m.put("methodId", inv.getMethodId());
        return m;
    }
}
