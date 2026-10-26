package com.hssc.backend;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminContentController {
    private final PublicContentService content;
    private final CurrentAccount current;

    public AdminContentController(PublicContentService content, CurrentAccount current) {
        this.content = content;
        this.current = current;
    }

    @GetMapping("/sponsors")
    public List<Api.SponsorResponse> sponsors() { return content.sponsors(true); }

    @PostMapping("/sponsors")
    public Api.SponsorResponse createSponsor(@Valid @RequestBody Api.SponsorRequest request, @AuthenticationPrincipal Jwt jwt) {
        return content.createSponsor(request, current.get(jwt));
    }

    @PutMapping("/sponsors/{id}")
    public Api.SponsorResponse updateSponsor(@PathVariable UUID id, @Valid @RequestBody Api.SponsorRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return content.updateSponsor(id, request, current.get(jwt));
    }

    @DeleteMapping("/sponsors/{id}")
    public Api.MessageResponse deleteSponsor(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        content.deleteSponsor(id, current.get(jwt));
        return new Api.MessageResponse("Sponsor deleted");
    }

    @GetMapping("/policies")
    public List<Api.ContentPolicyResponse> policies() { return content.policies(true); }

    @PostMapping("/policies")
    public Api.ContentPolicyResponse createPolicy(@Valid @RequestBody Api.ContentPolicyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return content.createPolicy(request, current.get(jwt));
    }

    @PutMapping("/policies/{id}")
    public Api.ContentPolicyResponse updatePolicy(@PathVariable UUID id, @Valid @RequestBody Api.ContentPolicyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return content.updatePolicy(id, request, current.get(jwt));
    }

    @DeleteMapping("/policies/{id}")
    public Api.MessageResponse deletePolicy(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        content.deletePolicy(id, current.get(jwt));
        return new Api.MessageResponse("Policy deleted");
    }
}
