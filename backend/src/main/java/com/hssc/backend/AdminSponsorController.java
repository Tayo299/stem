package com.hssc.backend;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/sponsorships")
public class AdminSponsorController {
    private final SponsorContributionRepository contributions;

    public AdminSponsorController(SponsorContributionRepository contributions) {
        this.contributions = contributions;
    }

    @GetMapping
    public List<Api.AdminSponsorContributionResponse> contributions() {
        return contributions.findAllByOrderByCreatedAtDesc().stream()
                .map(item -> new Api.AdminSponsorContributionResponse(item.getId(), item.getPackageCode(),
                        item.getPackageName(), item.getAmountNgn(), item.getOrganizationName(),
                        item.getContactName(), item.getEmail(), item.getPhone(), item.getWebsite(),
                        item.getMessage(), item.isPrivacyConsent(), item.getStatus().name(), item.getReference(),
                        item.getCreatedAt(), item.getPaidAt()))
                .toList();
    }
}
