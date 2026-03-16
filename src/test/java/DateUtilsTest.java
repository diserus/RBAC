import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    // ─── getCurrentDate ────────────────────────────────────────────────────

    @Test
    void getCurrentDate_matchesDatePattern() {
        String date = DateUtils.getCurrentDate();
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"),
                "Дата должна быть в формате YYYY-MM-DD, получено: " + date);
    }

    // ─── getCurrentDateTime ────────────────────────────────────────────────

    @Test
    void getCurrentDateTime_matchesDateTimePattern() {
        String dt = DateUtils.getCurrentDateTime();
        assertTrue(dt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Дата-время должны быть в формате YYYY-MM-DD HH:MM:SS, получено: " + dt);
    }

    // ─── isBefore ──────────────────────────────────────────────────────────

    @Test
    void isBefore_earlierDate_shouldReturnTrue() {
        assertTrue(DateUtils.isBefore("2024-01-01", "2024-06-01"));
    }

    @Test
    void isBefore_sameDate_shouldReturnFalse() {
        assertFalse(DateUtils.isBefore("2024-06-01", "2024-06-01"));
    }

    @Test
    void isBefore_laterDate_shouldReturnFalse() {
        assertFalse(DateUtils.isBefore("2024-12-31", "2024-01-01"));
    }

    @Test
    void isBefore_nullFirstArg_shouldReturnFalse() {
        assertFalse(DateUtils.isBefore(null, "2024-06-01"));
    }

    @Test
    void isBefore_nullSecondArg_shouldReturnFalse() {
        assertFalse(DateUtils.isBefore("2024-06-01", null));
    }

    // ─── isAfter ───────────────────────────────────────────────────────────

    @Test
    void isAfter_laterDate_shouldReturnTrue() {
        assertTrue(DateUtils.isAfter("2024-12-31", "2024-01-01"));
    }

    @Test
    void isAfter_sameDate_shouldReturnFalse() {
        assertFalse(DateUtils.isAfter("2024-06-01", "2024-06-01"));
    }

    @Test
    void isAfter_earlierDate_shouldReturnFalse() {
        assertFalse(DateUtils.isAfter("2024-01-01", "2024-06-01"));
    }

    @Test
    void isAfter_nullFirstArg_shouldReturnFalse() {
        assertFalse(DateUtils.isAfter(null, "2024-06-01"));
    }

    // ─── addDays ───────────────────────────────────────────────────────────

    @Test
    void addDays_addPositiveDays_shouldReturnCorrectDate() {
        assertEquals("2024-06-10", DateUtils.addDays("2024-06-01", 9));
    }

    @Test
    void addDays_addZeroDays_shouldReturnSameDate() {
        assertEquals("2024-06-01", DateUtils.addDays("2024-06-01", 0));
    }

    @Test
    void addDays_addNegativeDays_shouldReturnEarlierDate() {
        assertEquals("2024-05-25", DateUtils.addDays("2024-06-01", -7));
    }

    @Test
    void addDays_crossMonthBoundary_shouldReturnCorrectDate() {
        assertEquals("2024-07-01", DateUtils.addDays("2024-06-30", 1));
    }

    @Test
    void addDays_crossYearBoundary_shouldReturnCorrectDate() {
        assertEquals("2025-01-01", DateUtils.addDays("2024-12-31", 1));
    }

    @Test
    void addDays_nullDate_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> DateUtils.addDays(null, 5));
    }

    @Test
    void addDays_invalidFormat_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> DateUtils.addDays("not-a-date", 5));
    }

    // ─── formatRelativeTime ────────────────────────────────────────────────

    @Test
    void formatRelativeTime_today_shouldReturnToday() {
        String today = DateUtils.getCurrentDate();
        assertEquals("today", DateUtils.formatRelativeTime(today));
    }

    @Test
    void formatRelativeTime_yesterday_shouldReturnYesterday() {
        String yesterday = DateUtils.addDays(DateUtils.getCurrentDate(), -1);
        assertEquals("yesterday", DateUtils.formatRelativeTime(yesterday));
    }

    @Test
    void formatRelativeTime_tomorrow_shouldReturnTomorrow() {
        String tomorrow = DateUtils.addDays(DateUtils.getCurrentDate(), 1);
        assertEquals("tomorrow", DateUtils.formatRelativeTime(tomorrow));
    }

    @Test
    void formatRelativeTime_pastDays_shouldContainDaysAgo() {
        String past = DateUtils.addDays(DateUtils.getCurrentDate(), -5);
        assertEquals("5 days ago", DateUtils.formatRelativeTime(past));
    }

    @Test
    void formatRelativeTime_futureDays_shouldContainInDays() {
        String future = DateUtils.addDays(DateUtils.getCurrentDate(), 10);
        assertEquals("in 10 days", DateUtils.formatRelativeTime(future));
    }

    @Test
    void formatRelativeTime_null_shouldReturnUnknown() {
        assertEquals("unknown", DateUtils.formatRelativeTime(null));
    }
}