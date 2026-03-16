import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    // ─── formatTable ───────────────────────────────────────────────────────

    @Test
    void formatTable_containsHeaders() {
        String[] headers = {"Username", "Email"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"alice", "alice@example.com"});
        String table = FormatUtils.formatTable(headers, rows);
        assertTrue(table.contains("Username"));
        assertTrue(table.contains("Email"));
    }

    @Test
    void formatTable_containsRowData() {
        String[] headers = {"Name"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"alice"});
        String table = FormatUtils.formatTable(headers, rows);
        assertTrue(table.contains("alice"));
    }

    @Test
    void formatTable_columnWidthAdaptsToLongestValue() {
        String[] headers = {"X"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"very_long_value_here"});
        String table = FormatUtils.formatTable(headers, rows);
        assertTrue(table.contains("very_long_value_here"));
    }

    @Test
    void formatTable_emptyRows_stillShowsHeaders() {
        String[] headers = {"Col1", "Col2"};
        String table = FormatUtils.formatTable(headers, List.of());
        assertTrue(table.contains("Col1"));
        assertTrue(table.contains("Col2"));
    }

    @Test
    void formatTable_multipleRows_allPresent() {
        String[] headers = {"User"};
        List<String[]> rows = List.of(
                new String[]{"alice"},
                new String[]{"bob"},
                new String[]{"charlie"}
        );
        String table = FormatUtils.formatTable(headers, rows);
        assertTrue(table.contains("alice"));
        assertTrue(table.contains("bob"));
        assertTrue(table.contains("charlie"));
    }
    @Test
    void formatBox_containsText() {
        String result = FormatUtils.formatBox("Hello");
        assertTrue(result.contains("Hello"));
    }

    @Test
    void formatBox_hasBorderChars() {
        String result = FormatUtils.formatBox("Hello");
        assertTrue(result.contains("+"));
        assertTrue(result.contains("|"));
        assertTrue(result.contains("-"));
    }

    @Test
    void formatBox_multilineText_allLinesPresent() {
        String result = FormatUtils.formatBox("Line1\nLine2");
        assertTrue(result.contains("Line1"));
        assertTrue(result.contains("Line2"));
    }

    @Test
    void formatBox_emptyString_doesNotThrow() {
        assertDoesNotThrow(() -> FormatUtils.formatBox(""));
    }

    // ─── formatHeader ──────────────────────────────────────────────────────

    @Test
    void formatHeader_containsText() {
        String result = FormatUtils.formatHeader("ЗАГОЛОВОК");
        assertTrue(result.contains("ЗАГОЛОВОК"));
    }

    @Test
    void formatHeader_hasBoxChars() {
        String result = FormatUtils.formatHeader("TEST");
        assertTrue(result.contains("╔"));
        assertTrue(result.contains("╗"));
        assertTrue(result.contains("╚"));
        assertTrue(result.contains("╝"));
        assertTrue(result.contains("║"));
    }

    // ─── truncate ──────────────────────────────────────────────────────────

    @Test
    void truncate_shortString_returnsUnchanged() {
        assertEquals("hello", FormatUtils.truncate("hello", 10));
    }

    @Test
    void truncate_exactLength_returnsUnchanged() {
        assertEquals("hello", FormatUtils.truncate("hello", 5));
    }

    @Test
    void truncate_longString_appendsEllipsis() {
        String result = FormatUtils.truncate("hello world", 8);
        assertEquals("hello...", result);
        assertEquals(8, result.length());
    }

    @Test
    void truncate_null_returnsEmptyString() {
        assertEquals("", FormatUtils.truncate(null, 5));
    }

    @Test
    void truncate_maxLengthThree_returnsThreeChars() {
        String result = FormatUtils.truncate("abcdefg", 3);
        assertEquals(3, result.length());
    }

    // ─── padRight ──────────────────────────────────────────────────────────

    @Test
    void padRight_shortString_padded() {
        assertEquals("hi   ", FormatUtils.padRight("hi", 5));
    }

    @Test
    void padRight_exactLength_unchanged() {
        assertEquals("hello", FormatUtils.padRight("hello", 5));
    }

    @Test
    void padRight_longerThanLength_unchanged() {
        assertEquals("toolong", FormatUtils.padRight("toolong", 3));
    }

    @Test
    void padRight_null_returnsPadding() {
        assertEquals("     ", FormatUtils.padRight(null, 5));
    }

    // ─── padLeft ───────────────────────────────────────────────────────────

    @Test
    void padLeft_shortString_padded() {
        assertEquals("   hi", FormatUtils.padLeft("hi", 5));
    }

    @Test
    void padLeft_exactLength_unchanged() {
        assertEquals("hello", FormatUtils.padLeft("hello", 5));
    }

    @Test
    void padLeft_longerThanLength_unchanged() {
        assertEquals("toolong", FormatUtils.padLeft("toolong", 3));
    }

    @Test
    void padLeft_null_returnsPadding() {
        assertEquals("     ", FormatUtils.padLeft(null, 5));
    }
}