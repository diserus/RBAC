import java.sql.SQLOutput;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- ТЕСТЫ ДЛЯ USER ---");
        testUser();

        System.out.println("\n--- ТЕСТЫ ДЛЯ PERMISSION ---");
        testPermission();

        System.out.println("\n--- ТЕСТЫ ДЛЯ ROLE ---");
        testRole();
    }

    private static void testUser() {
        try {
            User user1 = User.validate("d1ser", "Konstantin Prozorenko", "prozorenko24@gmail.com");
            System.out.println("[PASS] User 1 (валидный): " + user1.format());
        } catch (IllegalArgumentException e) {
            System.out.println("[FAIL] User 1 (валидный) -> " + e.getMessage());
        }

        try {
            User user2 = User.validate("d1ser", "Konstantin Prozorenko", "prozorenkogmail.com");
            System.out.println("[FAIL] User 2 (невалидный email) -> должен был быть exception, но создался: " + user2.format());
        } catch (IllegalArgumentException e) {
            System.out.println("[PASS] User 2 (невалидный email): " + e.getMessage());
        }

        try {
            User user3 = User.validate("dd", "Konstantin Prozorenko", "prozorenko24@gmail.com");
            System.out.println("[FAIL] User 3 (короткий username) -> должен был быть exception, но создался: " + user3.format());
        } catch (IllegalArgumentException e) {
            System.out.println("[PASS] User 3 (короткий username): " + e.getMessage());
        }
    }

    private static void testPermission() {
        try {
            Permission p1 = new Permission("read", "USERS", "Allows reading users");
            System.out.println("[PASS] Permission 1: " + p1.format());
        } catch (IllegalArgumentException e) {
            System.out.println("[FAIL] Permission 1 -> " + e.getMessage());
        }

        try {
            Permission p = new Permission("READ ALL", "users", "Description");
            System.out.println("[FAIL] Permission 2 (пробел) -> должен был быть exception, но создался: " + p.format());
        } catch (IllegalArgumentException e) {
            System.out.println("[PASS] Permission 2 (пробел): " + e.getMessage());
        }

        Permission p2 = new Permission("WRITE", "reports", "Can write reports");
        System.out.println("matches WR/rep (ожидаю true): " + p2.matches("WR", "rep"));
        System.out.println("matches DELETE/rep (ожидаю false): " + p2.matches("DELETE", "rep"));
    }
    private static void testRole(){
        Role admin = null;
        try {
            admin = new Role("Administrator", "Full system access");
            System.out.println("[PASS] Role 1 (валидная) создана: " + admin);
            System.out.println("format():\n" + admin.format());
        } catch (IllegalArgumentException e) {
            System.out.println("[FAIL] Role 1 (валидная) -> " + e.getMessage());
        }

        try {
            new Role("   ", "desc");
            System.out.println("[FAIL] Role 2 (пустой name) -> ожидали исключение, но роль создалась");
        } catch (IllegalArgumentException e) {
            System.out.println("[PASS] Role 2 (пустой name): " + e.getMessage());
        }

        if (admin == null) {
            return;
        }

        Permission readUsers = new Permission("read", "USERS", "Allows reading users");
        admin.addPermission(readUsers);

        boolean has1 = admin.hasPermission(readUsers);
        System.out.println((has1 ? "[PASS]" : "[FAIL]") + " Role 3 hasPermission(Permission): " + has1);

        boolean has2 = admin.hasPermission("READ", "users");
        System.out.println((has2 ? "[PASS]" : "[FAIL]") + " Role 4 hasPermission(\"READ\",\"users\") ожидаю true: " + has2);

        boolean has3 = admin.hasPermission("READ ALL", "users");
        System.out.println((!has3 ? "[PASS]" : "[FAIL]") + " Role 5 hasPermission(\"READ ALL\",\"users\") ожидаю false: " + has3);

        admin.removePermission(readUsers);

        boolean hasAfterRemove = admin.hasPermission("READ", "users");
        System.out.println((!hasAfterRemove ? "[PASS]" : "[FAIL]") + " Role 6 removePermission: " + hasAfterRemove);

        Role admin2 = new Role("Administrator2", "Another role");
        boolean eq = admin.equals(admin2);
        System.out.println((!eq ? "[PASS]" : "[FAIL]") + " Role 7 equals: разные роли должны быть не равны -> " + eq);
    }
}