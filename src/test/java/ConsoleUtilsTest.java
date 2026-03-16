import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleUtilsTest {

    private Scanner scannerFrom(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    // ─── promptString ──────────────────────────────────────────────────────

    @Test
    void promptString_required_validInput_shouldReturn() {
        Scanner sc = scannerFrom("hello\n");
        assertEquals("hello", ConsoleUtils.promptString(sc, "Enter:", true));
    }

    @Test
    void promptString_notRequired_emptyInput_shouldReturnEmpty() {
        Scanner sc = scannerFrom("\n");
        assertEquals("", ConsoleUtils.promptString(sc, "Enter:", false));
    }

    @Test
    void promptString_required_emptyThenValid_shouldReturnValid() {
        // Первый ввод пустой, второй — валидный
        Scanner sc = scannerFrom("\nactual value\n");
        assertEquals("actual value", ConsoleUtils.promptString(sc, "Enter:", true));
    }

    @Test
    void promptString_trimsInput() {
        Scanner sc = scannerFrom("  trimmed  \n");
        assertEquals("trimmed", ConsoleUtils.promptString(sc, "Enter:", true));
    }

    // ─── promptInt ─────────────────────────────────────────────────────────

    @Test
    void promptInt_validNumber_shouldReturn() {
        Scanner sc = scannerFrom("3\n");
        assertEquals(3, ConsoleUtils.promptInt(sc, "Enter:", 1, 5));
    }

    @Test
    void promptInt_minBoundary_shouldReturn() {
        Scanner sc = scannerFrom("1\n");
        assertEquals(1, ConsoleUtils.promptInt(sc, "Enter:", 1, 5));
    }

    @Test
    void promptInt_maxBoundary_shouldReturn() {
        Scanner sc = scannerFrom("5\n");
        assertEquals(5, ConsoleUtils.promptInt(sc, "Enter:", 1, 5));
    }

    @Test
    void promptInt_tooSmallThenValid_shouldReturnValid() {
        Scanner sc = scannerFrom("0\n3\n");
        assertEquals(3, ConsoleUtils.promptInt(sc, "Enter:", 1, 5));
    }

    @Test
    void promptInt_tooBigThenValid_shouldReturnValid() {
        Scanner sc = scannerFrom("99\n2\n");
        assertEquals(2, ConsoleUtils.promptInt(sc, "Enter:", 1, 5));
    }

    @Test
    void promptInt_notANumberThenValid_shouldReturnValid() {
        Scanner sc = scannerFrom("abc\n4\n");
        assertEquals(4, ConsoleUtils.promptInt(sc, "Enter:", 1, 5));
    }

    // ─── promptYesNo ───────────────────────────────────────────────────────

    @Test
    void promptYesNo_да_shouldReturnTrue() {
        Scanner sc = scannerFrom("да\n");
        assertTrue(ConsoleUtils.promptYesNo(sc, "Confirm?"));
    }

    @Test
    void promptYesNo_нет_shouldReturnFalse() {
        Scanner sc = scannerFrom("нет\n");
        assertFalse(ConsoleUtils.promptYesNo(sc, "Confirm?"));
    }

    @Test
    void promptYesNo_yes_shouldReturnTrue() {
        Scanner sc = scannerFrom("yes\n");
        assertTrue(ConsoleUtils.promptYesNo(sc, "Confirm?"));
    }

    @Test
    void promptYesNo_no_shouldReturnFalse() {
        Scanner sc = scannerFrom("no\n");
        assertFalse(ConsoleUtils.promptYesNo(sc, "Confirm?"));
    }

    @Test
    void promptYesNo_y_shouldReturnTrue() {
        Scanner sc = scannerFrom("y\n");
        assertTrue(ConsoleUtils.promptYesNo(sc, "Confirm?"));
    }

    @Test
    void promptYesNo_invalidThenValid_shouldReturnCorrectly() {
        Scanner sc = scannerFrom("maybe\nда\n");
        assertTrue(ConsoleUtils.promptYesNo(sc, "Confirm?"));
    }

    @Test
    void promptYesNo_caseInsensitive_shouldWork() {
        Scanner sc = scannerFrom("ДА\n");
        assertTrue(ConsoleUtils.promptYesNo(sc, "Confirm?"));
    }

    // ─── promptChoice ──────────────────────────────────────────────────────

    @Test
    void promptChoice_validIndex_shouldReturnCorrectElement() {
        Scanner sc = scannerFrom("2\n");
        List<String> options = List.of("Alpha", "Beta", "Gamma");
        assertEquals("Beta", ConsoleUtils.promptChoice(sc, "Pick:", options));
    }

    @Test
    void promptChoice_firstElement_shouldReturn() {
        Scanner sc = scannerFrom("1\n");
        List<String> options = List.of("Only");
        assertEquals("Only", ConsoleUtils.promptChoice(sc, "Pick:", options));
    }

    @Test
    void promptChoice_lastElement_shouldReturn() {
        Scanner sc = scannerFrom("3\n");
        List<String> options = List.of("A", "B", "C");
        assertEquals("C", ConsoleUtils.promptChoice(sc, "Pick:", options));
    }

    @Test
    void promptChoice_outOfRangeThenValid_shouldReturnValid() {
        Scanner sc = scannerFrom("99\n1\n");
        List<String> options = List.of("Only");
        assertEquals("Only", ConsoleUtils.promptChoice(sc, "Pick:", options));
    }

    @Test
    void promptChoice_emptyList_shouldThrow() {
        Scanner sc = scannerFrom("1\n");
        assertThrows(IllegalArgumentException.class,
                () -> ConsoleUtils.promptChoice(sc, "Pick:", List.of()));
    }

    @Test
    void promptChoice_nullList_shouldThrow() {
        Scanner sc = scannerFrom("1\n");
        assertThrows(IllegalArgumentException.class,
                () -> ConsoleUtils.promptChoice(sc, "Pick:", null));
    }
}