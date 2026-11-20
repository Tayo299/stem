package com.hssc.backend;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sponsorships")
public class SponsorPaymentController {
    private final SponsorPaymentService payments;

    public SponsorPaymentController(SponsorPaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/checkout")
    public Api.SponsorCheckoutResponse checkout(@Valid @RequestBody Api.SponsorCheckoutRequest request) {
        return payments.initialize(request);
    }

    @GetMapping("/verify/{reference}")
    public Api.SponsorVerificationResponse verify(@PathVariable String reference) {
        return payments.verify(reference);
    }
}
