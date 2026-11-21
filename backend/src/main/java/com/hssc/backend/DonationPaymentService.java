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
public class DonationPaymentService {
    private final DonationContributionRepository contributions;
    private final RestClient paystack = RestClient.create();
    private final String secretKey;
    private final String callbackUrl;

    public DonationPaymentService(DonationContributionRepository contributions,
            @Value("${paystack.secret-key:}") String secretKey,
            @Value("${hssc.donation.callback-url:}") String callbackUrl) {
        this.contributions = contributions;
        this.secretKey = secretKey;
        this.callbackUrl = callbackUrl;
    }

    public Api.SponsorCheckoutResponse initialize(Api.DonationCheckoutRequest request) {
        requireConfiguration();
        long amountNgn;
        long amountKobo;
        try {
            amountNgn = request.amountNgn().longValueExact();
            amountKobo = Math.multiplyExact(amountNgn, 100);
        } catch (ArithmeticException exception) {
            throw ApiException.badRequest("Donation amount must be a whole number within the supported range.");
        }

        String reference = "HSSCD-" + UUID.randomUUID();
        DonationContribution contribution = contributions.save(new DonationContribution(reference, amountNgn,
                request.donorName().trim(), request.email().trim().toLowerCase(Locale.ROOT)));
        JsonNode response = paystackPost(Map.of(
                "email", contribution.getEmail(),
                "amount", Long.toString(amountKobo),
                "currency", "NGN",
                "reference", reference,
                "callback_url", callbackUrl,
                "metadata", Map.of("donation_id", reference)));
        JsonNode data = response.path("data");
        String authorizationUrl = data.path("authorization_url").asText("");
        if (!response.path("status").asBoolean(false)
                || !reference.equals(data.path("reference").asText())
                || !authorizationUrl.startsWith("https://checkout.paystack.com/")) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment provider could not start checkout. Please try again.");
        }
        return new Api.SponsorCheckoutResponse(authorizationUrl, reference);
    }

    public Api.DonationVerificationResponse verify(String reference) {
        if (reference == null || !reference.matches("HSSCD-[0-9a-fA-F-]{36}")) {
            throw ApiException.badRequest("Invalid donation reference");
        }
        DonationContribution contribution = contributions.findByReference(reference)
                .orElseThrow(() -> ApiException.notFound("Donation payment was not found"));
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
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment details did not match the donation amount.");
            }
            contribution.markPaid();
        } else if ("failed".equalsIgnoreCase(paymentStatus) || "abandoned".equalsIgnoreCase(paymentStatus)
                || "reversed".equalsIgnoreCase(paymentStatus)) {
            contribution.markFailed();
        }
        contributions.save(contribution);
        return verificationResponse(contribution);
    }

    private JsonNode paystackPost(Map<String, Object> payload) {
        try {
            JsonNode response = paystack.post()
                    .uri("https://api.paystack.co/transaction/initialize")
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
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Donations are not configured yet.");
        }
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Donation payment return URL is not configured yet.");
        }
        URI callback;
        try {
            callback = URI.create(callbackUrl);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Donation payment return URL is invalid.");
        }
        if (!"https".equalsIgnoreCase(callback.getScheme()) || callback.getHost() == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Donation payment return URL must be a valid HTTPS URL.");
        }
    }

    private Api.DonationVerificationResponse verificationResponse(DonationContribution contribution) {
        return new Api.DonationVerificationResponse(contribution.getStatus().name(), contribution.getAmountNgn());
    }
}
