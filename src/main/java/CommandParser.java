import java.util.*;

public class CommandParser {
    private final Map<String, Command>     commands            = new LinkedHashMap<>();
    private final Map<String, String>      commandDescriptions = new LinkedHashMap<>();

    public void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command cmd = commands.get(commandName);
        if (cmd == null) {
            System.out.println("Неизвестная команда: '" + commandName + "'. Введите 'help' для справки.");
            return;
        }
        try {
            cmd.execute(scanner, system);
        } catch (Exception e) {
            System.out.println("Ошибка выполнения команды: " + e.getMessage());
        }
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.isBlank()) return;
        String commandName = input.trim().split("\\s+")[0].toLowerCase();
        executeCommand(commandName, scanner, system);
    }

    public void printHelp() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    ДОСТУПНЫЕ КОМАНДЫ                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        commandDescriptions.forEach((name, desc) ->
                System.out.printf("║  %-20s — %-33s║%n", name, desc)
        );
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }
}
