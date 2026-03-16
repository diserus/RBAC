import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    // ANSI-коды цветов (опционально)
    public static final String RESET  = "\u001B[0m";
    public static final String BOLD   = "\u001B[1m";
    public static final String GREEN  = "\u001B[32m";
    public static final String RED    = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN   = "\u001B[36m";

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(CYAN + message + RESET + " ");
            String value = scanner.nextLine().trim();
            if (!required || !value.isEmpty()) {
                return value;
            }
            System.out.println(RED + "✗ Поле не может быть пустым. Попробуйте снова." + RESET);
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(CYAN + message + " [" + min + "–" + max + "]: " + RESET + " ");
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
                System.out.printf(RED + "✗ Введите число от %d до %d.%n" + RESET, min, max);
            } catch (NumberFormatException e) {
                System.out.println(RED + "✗ Это не число. Попробуйте снова." + RESET);
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(CYAN + message + " (да/нет): " + RESET + " ");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (answer.equals("да") || answer.equals("yes") || answer.equals("y")) return true;
            if (answer.equals("нет") || answer.equals("no") || answer.equals("n")) return false;
            System.out.println(RED + "✗ Введите 'да' или 'нет'." + RESET);
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Список вариантов не может быть пустым.");
        }
        System.out.println(BOLD + message + RESET);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, options.get(i));
        }
        int idx = promptInt(scanner, "Ваш выбор", 1, options.size());
        return options.get(idx - 1);
    }
}