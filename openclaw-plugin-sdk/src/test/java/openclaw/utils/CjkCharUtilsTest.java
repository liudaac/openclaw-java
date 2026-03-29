package openclaw.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CjkCharUtils.
 */
class CjkCharUtilsTest {

    @Test
    void testIsCjkUnified() {
        // Chinese characters
        assertTrue(CjkCharUtils.isCjkUnified('中'));
        assertTrue(CjkCharUtils.isCjkUnified('文'));
        assertTrue(CjkCharUtils.isCjkUnified('测'));
        assertTrue(CjkCharUtils.isCjkUnified('试'));
        
        // Non-CJK characters
        assertFalse(CjkCharUtils.isCjkUnified('A'));
        assertFalse(CjkCharUtils.isCjkUnified('1'));
        assertFalse(CjkCharUtils.isCjkUnified('!'));
        assertFalse(CjkCharUtils.isCjkUnified(' '));
    }

    @Test
    void testIsHiragana() {
        // Japanese Hiragana
        assertTrue(CjkCharUtils.isHiragana('あ'));
        assertTrue(CjkCharUtils.isHiragana('い'));
        assertTrue(CjkCharUtils.isHiragana('う'));
        assertTrue(CjkCharUtils.isHiragana('か'));
        assertTrue(CjkCharUtils.isHiragana('き'));
        
        // Non-Hiragana
        assertFalse(CjkCharUtils.isHiragana('ア')); // Katakana
        assertFalse(CjkCharUtils.isHiragana('中')); // Chinese
        assertFalse(CjkCharUtils.isHiragana('A'));
    }

    @Test
    void testIsKatakana() {
        // Japanese Katakana
        assertTrue(CjkCharUtils.isKatakana('ア'));
        assertTrue(CjkCharUtils.isKatakana('イ'));
        assertTrue(CjkCharUtils.isKatakana('ウ'));
        assertTrue(CjkCharUtils.isKatakana('カ'));
        assertTrue(CjkCharUtils.isKatakana('キ'));
        
        // Non-Katakana
        assertFalse(CjkCharUtils.isKatakana('あ')); // Hiragana
        assertFalse(CjkCharUtils.isKatakana('中')); // Chinese
        assertFalse(CjkCharUtils.isKatakana('A'));
    }

    @Test
    void testIsHangul() {
        // Korean Hangul
        assertTrue(CjkCharUtils.isHangul('가'));
        assertTrue(CjkCharUtils.isHangul('나'));
        assertTrue(CjkCharUtils.isHangul('다'));
        assertTrue(CjkCharUtils.isHangul('한'));
        assertTrue(CjkCharUtils.isHangul('글'));
        
        // Non-Hangul
        assertFalse(CjkCharUtils.isHangul('中')); // Chinese
        assertFalse(CjkCharUtils.isHangul('あ')); // Hiragana
        assertFalse(CjkCharUtils.isHangul('A'));
    }

    @Test
    void testIsCjk() {
        // All CJK types
        assertTrue(CjkCharUtils.isCjk('中')); // Chinese
        assertTrue(CjkCharUtils.isCjk('あ')); // Hiragana
        assertTrue(CjkCharUtils.isCjk('ア')); // Katakana
        assertTrue(CjkCharUtils.isCjk('가')); // Hangul
        
        // Non-CJK
        assertFalse(CjkCharUtils.isCjk('A'));
        assertFalse(CjkCharUtils.isCjk('1'));
        assertFalse(CjkCharUtils.isCjk('!'));
    }

    @Test
    void testEstimateTokens() {
        // Empty/null
        assertEquals(0, CjkCharUtils.estimateTokens(null));
        assertEquals(0, CjkCharUtils.estimateTokens(""));
        
        // CJK only: 1 token per char
        assertEquals(1, CjkCharUtils.estimateTokens("中"));
        assertEquals(2, CjkCharUtils.estimateTokens("中文"));
        assertEquals(4, CjkCharUtils.estimateTokens("中文测试"));
        
        // Non-CJK: ~1 token per 4 chars
        assertEquals(1, CjkCharUtils.estimateTokens("test"));
        assertEquals(2, CjkCharUtils.estimateTokens("hello")); // 5 chars -> ceil(5/4) = 2
        assertEquals(3, CjkCharUtils.estimateTokens("hello world")); // 10 chars, whitespace ignored -> ceil(10/4) = 3
        
        // Mixed
        assertEquals(2, CjkCharUtils.estimateTokens("中test")); // 1 CJK + 1 (4 chars / 4)
        assertEquals(3, CjkCharUtils.estimateTokens("中文test")); // 2 CJK + 1
    }

    @Test
    void testGetWeightedLength() {
        // Empty/null
        assertEquals(0, CjkCharUtils.getWeightedLength(null));
        assertEquals(0, CjkCharUtils.getWeightedLength(""));
        
        // CJK weight: 3
        assertEquals(3, CjkCharUtils.getWeightedLength("中"));
        assertEquals(6, CjkCharUtils.getWeightedLength("中文"));
        assertEquals(12, CjkCharUtils.getWeightedLength("中文测试"));
        
        // Non-CJK weight: 1
        assertEquals(1, CjkCharUtils.getWeightedLength("a"));
        assertEquals(4, CjkCharUtils.getWeightedLength("test"));
        assertEquals(5, CjkCharUtils.getWeightedLength("hello"));
        
        // Mixed
        assertEquals(7, CjkCharUtils.getWeightedLength("中test")); // 3 + 4
        assertEquals(10, CjkCharUtils.getWeightedLength("中文test")); // 6 + 4
    }

    @Test
    void testEstimateTokensWithWhitespace() {
        // Whitespace should be ignored in non-CJK count
        assertEquals(1, CjkCharUtils.estimateTokens("a b c d")); // 4 non-whitespace chars
        assertEquals(2, CjkCharUtils.estimateTokens("test test")); // 8 chars, whitespace ignored
    }
}
