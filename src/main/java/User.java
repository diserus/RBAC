public record User(String username, String fullName, String email) {

    public User{
        if (username == null || fullName == null || email == null) {
            throw new IllegalArgumentException("Все поля должны быть не null");
        }

        if (username.trim().isEmpty() || fullName.trim().isEmpty() || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Все поля должны быть не пустыми строками (включая пробелы)");
        }

        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException(
                    "username должен содержать только латинские буквы, цифры и подчёркивание, также должен быть от 3 до 20 символов "
            );
        }

        if (!email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new IllegalArgumentException(
                    "email должен соответствовать базовому формату email (содержать @ и точку после @)"
            );
        }
    }
    public  static User create(String username, String fullName, String
            email){
        return new User(username,fullName,email);
   }
   public String format(){
        return String.format("%s (%s) <%s>",username,fullName,email);
   }
}
