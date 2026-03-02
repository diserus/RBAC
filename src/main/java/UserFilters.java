public class UserFilters {

    public static UserFilter byUsername(String username) {
        return user -> user.username().equals(username);
    }

    public static UserFilter byUsernameContains(String substring) {
        return user -> user.username().toLowerCase().contains(substring.toLowerCase());
    }

    public static UserFilter byEmail(String email) {
        return user -> user.email().equals(email);
    }

    public static UserFilter byEmailDomain(String domain) {
        return user -> user.email().endsWith(domain);
    }

    public static UserFilter byFullNameContains(String substring) {
        return user -> user.fullName().toLowerCase().contains(substring.toLowerCase());
    }
}
