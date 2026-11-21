package com.hssc.backend;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donations")
public class DonationPaymentController {
    private final DonationPaymentService payments;

    public DonationPaymentController(DonationPaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/checkout")
    public Api.SponsorCheckoutResponse checkout(@Valid @RequestBody Api.DonationCheckoutRequest request) {
        return payments.initialize(request);
    }

    @GetMapping("/verify/{reference}")
    public Api.DonationVerificationResponse verify(@PathVariable String reference) {
        return payments.verify(reference);
    }
}
