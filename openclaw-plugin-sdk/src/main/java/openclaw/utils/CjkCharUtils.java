package openclaw.utils;

/**
 * Utilities for CJK (Chinese, Japanese, Korean) character handling.
 */
public final class CjkCharUtils {
    
    private CjkCharUtils() {}
    
    /**
     * Check if a character is CJK.
     */
    public static boolean isCjk(char c) {
        return isCjkUnified(c) || isHiragana(c) || isKatakana(c) || isHangul(c);
    }
    
    /**
     * Check if CJK Unified Ideograph.
     */
    public static boolean isCjkUnified(char c) {
        return (c >= '\u4e00' && c <= '\u9fff') ||
               (c >= '\u3400' && c <= '\u4dbf');
    }
    
    /**
     * Check if Hiragana (Japanese).
     */
    public static boolean isHiragana(char c) {
        return c >= '\u3040' && c <= '\u309f';
    }
    
    /**
     * Check if Katakana (Japanese).
     */
    public static boolean isKatakana(char c) {
        return c >= '\u30a0' && c <= '\u30ff';
    }
    
    /**
     * Check if Hangul (Korean).
     */
    public static boolean isHangul(char c) {
        return (c >= '\uac00' && c <= '\ud7af') ||
               (c >= '\u1100' && c <= '\u11ff');
    }
    
    /**
     * Estimate token count.
     * CJK: ~1 token per char
     * Non-CJK: ~1 token per 4 chars
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        
        int cjkCount = 0, nonCjkCount = 0;
        for (char c : text.toCharArray()) {
            if (isCjk(c)) cjkCount++;
            else if (!Character.isWhitespace(c)) nonCjkCount++;
        }
        
        return cjkCount + (nonCjkCount + 3) / 4;
    }
    
    /**
     * Get weighted length for chunking.
     * CJK weight: 3, others: 1
     */
    public static int getWeightedLength(String text) {
        if (text == null || text.isEmpty()) return 0;
        int length = 0;
        for (char c : text.toCharArray()) {
            length += isCjk(c) ? 3 : 1;
        }
        return length;
    }
}
