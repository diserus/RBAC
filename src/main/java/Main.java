import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.startScheduledMaintenance(10);

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        Scanner scanner = new Scanner(System.in);
        System.out.println("RBAC System запущена. Введите 'help' для справки.");
        System.out.println("Текущий пользователь: " + system.getCurrentUser());

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine();
            parser.parseAndExecute(input, scanner, system);
        }
    }
}
