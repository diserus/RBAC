//TIP Чтобы <b>запустить</b> код, нажмите <shortcut actionId="Run"/> или
// нажмите на значок <icon src="AllIcons.Actions.Execute"/> в поле.
public class Main {
    public static void main(String[] args) {
        try {
            User user1 = User.validate("d1ser", "Konstantin Prozorenko", "prozorenko24@gmail.com");
            System.out.println("Тест 1 (валидный пользователь): " + user1.format());
        } catch (Exception e) {
            System.err.println("Тест 1 провален: " + e.getMessage());
        }
        try {
            User user1 = User.validate("d1ser", "Konstantin Prozorenko", "prozorenkogmail.com");
            System.out.println("Тест 2 (инвалидный email): " + user1.format());
        } catch (Exception e) {
            System.err.println("Тест 2 провален: " + e.getMessage());
        }
        try {
            User user1 = User.validate("dd", "Konstantin Prozorenko", "prozorenko24@gmail.com");
            System.out.println("Тест 3 (инвалидное имя пользователя): " + user1.format());
        } catch (Exception e) {
            System.err.println("Тест 3 провален: " + e.getMessage());
        }
    }
}