package com.hssc.backend;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class SponsorPaymentService {
    private final SponsorContributionRepository contributions;
    private final RestClient paystack = RestClient.create();
    private final String secretKey;
    private final String callbackUrl;

    public SponsorPaymentService(SponsorContributionRepository contributions,
            @Value("${paystack.secret-key:}") String secretKey,
            @Value("${hssc.sponsorship.callback-url:}") String callbackUrl) {
        this.contributions = contributions;
        this.secretKey = secretKey;
        this.callbackUrl = callbackUrl;
    }

    public Api.SponsorCheckoutResponse initialize(Api.SponsorCheckoutRequest request) {
        requireConfiguration();
        SponsorPackage plan = SponsorPackage.fromCode(request.packageCode());
        String website = normalizeWebsite(request.website());
        String reference = "HSSC-" + UUID.randomUUID();
        SponsorContribution contribution = contributions.save(new SponsorContribution(reference, plan,
                request.organizationName().trim(), request.contactName().trim(), request.email().trim().toLowerCase(Locale.ROOT),
                request.phone().trim(), website, trimToNull(request.message()), request.privacyConsent()));

        JsonNode response = paystackPost("/transaction/initialize", Map.of(
                "email", contribution.getEmail(),
                "amount", Long.toString(plan.amountKobo()),
                "currency", "NGN",
                "reference", reference,
                "callback_url", callbackUrl,
                "metadata", Map.of("contribution_id", contribution.getId().toString(),
                        "package_code", plan.name())));
        JsonNode data = response.path("data");
        String authorizationUrl = data.path("authorization_url").asText("");
        if (!response.path("status").asBoolean(false)
                || !reference.equals(data.path("reference").asText())
                || !authorizationUrl.startsWith("https://checkout.paystack.com/")) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment provider could not start checkout. Please try again.");
        }
        return new Api.SponsorCheckoutResponse(authorizationUrl, reference);
    }

    public Api.SponsorVerificationResponse verify(String reference) {
        if (reference == null || !reference.matches("HSSC-[0-9a-fA-F-]{36}")) {
            throw ApiException.badRequest("Invalid payment reference");
        }
        SponsorContribution contribution = contributions.findByReference(reference)
                .orElseThrow(() -> ApiException.notFound("Sponsorship payment was not found"));
        if (contribution.getStatus() == SponsorPaymentStatus.PAID) return verificationResponse(contribution);
        requireConfiguration();

        JsonNode response;
        try {
            response = paystack.get()
                    .uri("https://api.paystack.co/transaction/verify/{reference}", reference)
                    .header("Authorization", "Bearer " + secretKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not verify the payment with the provider. Please retry.");
        }

        JsonNode data = response == null ? null : response.path("data");
        if (response == null || !response.path("status").asBoolean(false) || data == null || data.isMissingNode()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment provider returned an invalid verification response.");
        }
        String paymentStatus = data.path("status").asText("");
        if ("success".equalsIgnoreCase(paymentStatus)) {
            if (!reference.equals(data.path("reference").asText())
                    || !"NGN".equalsIgnoreCase(data.path("currency").asText())
                    || data.path("amount").asLong(-1) != contribution.getAmountNgn() * 100) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment details did not match the selected sponsorship package.");
            }
            contribution.markPaid();
        } else if ("failed".equalsIgnoreCase(paymentStatus) || "abandoned".equalsIgnoreCase(paymentStatus)
                || "reversed".equalsIgnoreCase(paymentStatus)) {
            contribution.markFailed();
        }
        contributions.save(contribution);
        return verificationResponse(contribution);
    }

    private JsonNode paystackPost(String path, Map<String, Object> payload) {
        try {
            JsonNode response = paystack.post()
                    .uri("https://api.paystack.co" + path)
                    .header("Authorization", "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment provider returned an empty response.");
            return response;
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not start payment checkout. Please try again.");
        }
    }

    private void requireConfiguration() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Sponsor payments are not configured yet.");
        }
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Sponsor payment return URL is not configured yet.");
        }
        URI callback;
        try {
            callback = URI.create(callbackUrl);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Sponsor payment return URL is invalid.");
        }
        if (!"https".equalsIgnoreCase(callback.getScheme()) || callback.getHost() == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Sponsor payment return URL must be a valid HTTPS URL.");
        }
    }

    private Api.SponsorVerificationResponse verificationResponse(SponsorContribution contribution) {
        return new Api.SponsorVerificationResponse(contribution.getStatus().name(),
                contribution.getPackageName(), contribution.getAmountNgn());
    }

    private String normalizeWebsite(String website) {
        String value = trimToNull(website);
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("Website must be a valid http or https URL");
        }
        return value;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
