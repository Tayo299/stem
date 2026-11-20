package com.hssc.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SponsorPackageTest {
    @Test
    void packagesUseServerDefinedNairaAmountsAndKoboAmounts() {
        assertEquals(10_000_000L, SponsorPackage.fromCode("BRONZE").amountNaira());
        assertEquals(1_000_000_000L, SponsorPackage.fromCode("bronze").amountKobo());
        assertEquals(25_000_000L, SponsorPackage.fromCode("SILVER").amountNaira());
        assertEquals(2_500_000_000L, SponsorPackage.fromCode("SILVER").amountKobo());
        assertEquals(50_000_000L, SponsorPackage.fromCode("GOLD").amountNaira());
        assertEquals(5_000_000_000L, SponsorPackage.fromCode("GOLD").amountKobo());
    }

    @Test
    void rejectsUnknownPackageCodes() {
        assertThrows(ApiException.class, () -> SponsorPackage.fromCode("CUSTOM"));
    }
}
