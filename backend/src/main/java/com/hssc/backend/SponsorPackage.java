package com.hssc.backend;

import java.util.Arrays;

public enum SponsorPackage {
    BRONZE("Bronze Partner", 10_000_000L),
    SILVER("Silver Partner", 25_000_000L),
    GOLD("Gold Partner", 50_000_000L);

    private final String displayName;
    private final long amountNaira;

    SponsorPackage(String displayName, long amountNaira) {
        this.displayName = displayName;
        this.amountNaira = amountNaira;
    }

    public String displayName() { return displayName; }
    public long amountNaira() { return amountNaira; }
    public long amountKobo() { return amountNaira * 100; }

    public static SponsorPackage fromCode(String code) {
        return Arrays.stream(values())
                .filter(plan -> plan.name().equalsIgnoreCase(code == null ? "" : code.trim()))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("Choose a valid sponsorship package"));
    }
}
