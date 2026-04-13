import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Object mutationLock = new Object();

    @Override
    public void add(User user) {
        if (user == null) throw new IllegalArgumentException("user не может быть null");
        synchronized (mutationLock) {
            if (users.containsKey(user.username())) {
                throw new IllegalStateException("Пользователь '" + user.username() + "' уже существует");
            }
            users.put(user.username(), user);
        }
    }

    @Override
    public boolean remove(User user) {
        if (user == null) return false;
        return users.remove(user.username()) != null;
    }
    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        synchronized (mutationLock) {
            users.clear();
        }
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        return users.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findByFilterParallel(UserFilter filter) {
        return users.values().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        return users.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        synchronized (mutationLock) {
            if (!users.containsKey(username)) {
                throw new NoSuchElementException("Пользователь '" + username + "' не найден");
            }
            User updated = User.create(username, newFullName, newEmail);
            users.put(username, updated);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserManager that = (UserManager) o;
        return Objects.equals(users, that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }
}
