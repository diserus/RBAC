import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {
    private CommandParser parser;
    private RBACSystem system;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();
    }

    private Scanner scannerFrom(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    void registerCommand_shouldAddCommand() {
        parser.registerCommand("test", "Test command", (sc, sys) -> {});
        // Не должно бросать исключение при выполнении
        assertDoesNotThrow(() ->
                parser.executeCommand("test", scannerFrom(""), system)
        );
    }

    @Test
    void executeCommand_unknownCommand_shouldNotThrow() {
        assertDoesNotThrow(() ->
                parser.executeCommand("unknown-cmd", scannerFrom(""), system)
        );
    }

    @Test
    void executeCommand_unknownCommand_shouldPrintMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        parser.executeCommand("unknown-cmd", scannerFrom(""), system);
        System.setOut(System.out);
        assertTrue(out.toString().contains("unknown-cmd"));
    }

    @Test
    void parseAndExecute_shouldExtractFirstWordAsCommand() {
        List<String> called = new ArrayList<>();
        parser.registerCommand("ping", "Test", (sc, sys) -> called.add("ping"));
        parser.parseAndExecute("ping some extra args", scannerFrom(""), system);
        assertEquals(1, called.size());
    }

    @Test
    void parseAndExecute_blankInput_shouldNotThrow() {
        assertDoesNotThrow(() ->
                parser.parseAndExecute("   ", scannerFrom(""), system)
        );
    }

    @Test
    void parseAndExecute_nullInput_shouldNotThrow() {
        assertDoesNotThrow(() ->
                parser.parseAndExecute(null, scannerFrom(""), system)
        );
    }

    @Test
    void parseAndExecute_caseInsensitive_shouldFindCommand() {
        List<String> called = new ArrayList<>();
        parser.registerCommand("help", "Help", (sc, sys) -> called.add("help"));
        parser.parseAndExecute("HELP", scannerFrom(""), system);
        assertEquals(1, called.size());
    }

    @Test
    void printHelp_shouldContainRegisteredCommands() {
        parser.registerCommand("foo", "Foo description", (sc, sys) -> {});
        parser.registerCommand("bar", "Bar description", (sc, sys) -> {});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        parser.printHelp();
        System.setOut(System.out);
        String output = out.toString();
        assertTrue(output.contains("foo"));
        assertTrue(output.contains("bar"));
        assertTrue(output.contains("Foo description"));
    }

    @Test
    void executeCommand_exceptionInCommand_shouldBeCaught() {
        parser.registerCommand("boom", "Throws", (sc, sys) -> {
            throw new RuntimeException("test error");
        });
        assertDoesNotThrow(() ->
                parser.executeCommand("boom", scannerFrom(""), system)
        );
    }

    @Test
    void executeCommand_exceptionMessage_shouldBePrinted() {
        parser.registerCommand("boom", "Throws", (sc, sys) -> {
            throw new RuntimeException("test error");
        });
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        parser.executeCommand("boom", scannerFrom(""), system);
        System.setOut(System.out);
        assertTrue(out.toString().contains("test error"));
    }
}
