import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    // ─── isValidUsername ───────────────────────────────────────────────────

    @Test
    void isValidUsername_validLowercase_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidUsername("alice"));
    }

    @Test
    void isValidUsername_validWithDigitsAndUnderscore_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidUsername("user_123"));
    }

    @Test
    void isValidUsername_exactlyThreeChars_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidUsername("abc"));
    }

    @Test
    void isValidUsername_exactlyTwentyChars_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidUsername("a".repeat(20)));
    }

    @Test
    void isValidUsername_tooShort_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidUsername("ab"));
    }

    @Test
    void isValidUsername_tooLong_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidUsername("a".repeat(21)));
    }

    @Test
    void isValidUsername_withSpace_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidUsername("user name"));
    }

    @Test
    void isValidUsername_withCyrillic_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidUsername("пользователь"));
    }

    @Test
    void isValidUsername_null_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidUsername(null));
    }

    @Test
    void isValidUsername_empty_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidUsername(""));
    }

    // ─── isValidEmail ──────────────────────────────────────────────────────

    @Test
    void isValidEmail_standardEmail_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
    }

    @Test
    void isValidEmail_withSubdomain_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidEmail("admin@mail.company.org"));
    }

    @Test
    void isValidEmail_noAtSign_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidEmail("userexample.com"));
    }

    @Test
    void isValidEmail_noDotAfterAt_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidEmail("user@examplecom"));
    }

    @Test
    void isValidEmail_doubleAt_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidEmail("user@@example.com"));
    }

    @Test
    void isValidEmail_null_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    void isValidEmail_empty_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidEmail(""));
    }

    @Test
    void isValidEmail_withLeadingSpaces_shouldReturnTrue() {
        // trim() применяется внутри
        assertTrue(ValidationUtils.isValidEmail("  user@example.com  "));
    }

    // ─── isValidDate ───────────────────────────────────────────────────────

    @Test
    void isValidDate_correctFormat_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidDate("2024-06-15"));
    }

    @Test
    void isValidDate_edgeDateFirstDay_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidDate("2024-01-01"));
    }

    @Test
    void isValidDate_edgeDateLastDay_shouldReturnTrue() {
        assertTrue(ValidationUtils.isValidDate("2024-12-31"));
    }

    @Test
    void isValidDate_wrongSeparator_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidDate("2024/06/15"));
    }

    @Test
    void isValidDate_invalidMonth_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidDate("2024-13-01"));
    }

    @Test
    void isValidDate_invalidDay_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidDate("2024-06-32"));
    }

    @Test
    void isValidDate_null_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidDate(null));
    }

    @Test
    void isValidDate_reversedFormat_shouldReturnFalse() {
        assertFalse(ValidationUtils.isValidDate("15-06-2024"));
    }

    // ─── normalizeString ───────────────────────────────────────────────────

    @Test
    void normalizeString_trimsLeadingAndTrailingSpaces() {
        assertEquals("hello", ValidationUtils.normalizeString("  hello  "));
    }

    @Test
    void normalizeString_collapsesInternalSpaces() {
        assertEquals("hello world", ValidationUtils.normalizeString("hello   world"));
    }

    @Test
    void normalizeString_convertsToLowercase() {
        assertEquals("hello world", ValidationUtils.normalizeString("Hello WORLD"));
    }

    @Test
    void normalizeString_null_shouldReturnNull() {
        assertNull(ValidationUtils.normalizeString(null));
    }

    @Test
    void normalizeString_alreadyNormalized_shouldReturnSame() {
        assertEquals("abc", ValidationUtils.normalizeString("abc"));
    }

    // ─── requireNonEmpty ───────────────────────────────────────────────────

    @Test
    void requireNonEmpty_validValue_shouldNotThrow() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty("hello", "field"));
    }

    @Test
    void requireNonEmpty_null_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty(null, "myField"));
        assertTrue(ex.getMessage().contains("myField"));
    }

    @Test
    void requireNonEmpty_emptyString_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty("", "myField"));
    }

    @Test
    void requireNonEmpty_blankString_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty("   ", "myField"));
    }
}