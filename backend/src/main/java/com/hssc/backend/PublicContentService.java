package com.hssc.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PublicContentService {
    private final SponsorRepository sponsors;
    private final ContentPolicyRepository policies;
    private final AuditService audit;

    public PublicContentService(SponsorRepository sponsors, ContentPolicyRepository policies, AuditService audit) {
        this.sponsors = sponsors;
        this.policies = policies;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Api.SponsorResponse> sponsors(boolean all) {
        return (all ? sponsors.findAllByOrderByDisplayOrderAscNameAsc() : sponsors.findByActiveTrueOrderByDisplayOrderAscNameAsc())
                .stream().map(this::sponsorResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Api.ContentPolicyResponse> policies(boolean all) {
        return (all ? policies.findAllByOrderByDisplayOrderAscTitleAsc() : policies.findByActiveTrueOrderByDisplayOrderAscTitleAsc())
                .stream().map(this::policyResponse).toList();
    }

    @Transactional
    public Api.SponsorResponse createSponsor(Api.SponsorRequest request, UserAccount actor) {
        validateWebsite(request.websiteUrl());
        Sponsor sponsor = sponsors.save(new Sponsor(request.name().trim(), trim(request.organization()), trim(request.description()),
                trim(request.websiteUrl()), request.displayOrder(), request.active()));
        audit.record(actor, "CREATE", "Sponsor", sponsor.getId().toString());
        return sponsorResponse(sponsor);
    }

    @Transactional
    public Api.SponsorResponse updateSponsor(UUID id, Api.SponsorRequest request, UserAccount actor) {
        validateWebsite(request.websiteUrl());
        Sponsor sponsor = sponsors.findById(id).orElseThrow(() -> ApiException.notFound("Sponsor not found"));
        sponsor.update(request.name().trim(), trim(request.organization()), trim(request.description()),
                trim(request.websiteUrl()), request.displayOrder(), request.active());
        audit.record(actor, "UPDATE", "Sponsor", id.toString());
        return sponsorResponse(sponsor);
    }

    @Transactional
    public void deleteSponsor(UUID id, UserAccount actor) {
        Sponsor sponsor = sponsors.findById(id).orElseThrow(() -> ApiException.notFound("Sponsor not found"));
        sponsors.delete(sponsor);
        audit.record(actor, "DELETE", "Sponsor", id.toString());
    }

    @Transactional
    public Api.ContentPolicyResponse createPolicy(Api.ContentPolicyRequest request, UserAccount actor) {
        String slug = validSlug(request.slug());
        ContentPolicy policy = policies.save(new ContentPolicy(slug, request.title().trim(), request.content().trim(),
                request.active(), request.displayOrder()));
        audit.record(actor, "CREATE", "ContentPolicy", policy.getId().toString());
        return policyResponse(policy);
    }

    @Transactional
    public Api.ContentPolicyResponse updatePolicy(UUID id, Api.ContentPolicyRequest request, UserAccount actor) {
        ContentPolicy policy = policies.findById(id).orElseThrow(() -> ApiException.notFound("Policy not found"));
        policy.update(validSlug(request.slug()), request.title().trim(), request.content().trim(),
                request.active(), request.displayOrder());
        audit.record(actor, "UPDATE", "ContentPolicy", id.toString());
        return policyResponse(policy);
    }

    @Transactional
    public void deletePolicy(UUID id, UserAccount actor) {
        ContentPolicy policy = policies.findById(id).orElseThrow(() -> ApiException.notFound("Policy not found"));
        policies.delete(policy);
        audit.record(actor, "DELETE", "ContentPolicy", id.toString());
    }

    private Api.SponsorResponse sponsorResponse(Sponsor s) {
        return new Api.SponsorResponse(s.getId(), s.getName(), s.getOrganization(), s.getDescription(),
                s.getWebsiteUrl(), s.getDisplayOrder(), s.isActive());
    }

    private Api.ContentPolicyResponse policyResponse(ContentPolicy p) {
        return new Api.ContentPolicyResponse(p.getId(), p.getSlug(), p.getTitle(), p.getContent(), p.isActive(), p.getDisplayOrder());
    }

    private String validSlug(String value) {
        String slug = value.trim().toLowerCase(Locale.ROOT);
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) throw ApiException.badRequest("Policy slug must contain lowercase letters, numbers, and hyphens");
        return slug;
    }

    private void validateWebsite(String value) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = URI.create(value.trim());
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null)
                throw ApiException.badRequest("Sponsor website must be an absolute HTTP or HTTPS URL without credentials");
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Sponsor website must be a valid HTTP or HTTPS URL");
        }
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
