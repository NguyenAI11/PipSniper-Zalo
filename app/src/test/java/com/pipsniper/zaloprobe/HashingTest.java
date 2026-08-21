package com.pipsniper.zaloprobe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HashingTest {
    @Test
    public void sha256IsStable() {
        assertEquals(Hashing.sha256("BUY XAUUSD"), Hashing.sha256("BUY XAUUSD"));
        assertFalse(Hashing.sha256("BUY XAUUSD").equals(Hashing.sha256("SELL XAUUSD")));
    }

    @Test
    public void normalizeTextKeepsMeaningfulLineBreaks() {
        assertEquals("Buy vàng\nSL 4488", Hashing.normalizeText(" Buy   vàng\r\nSL 4488 "));
    }
}
