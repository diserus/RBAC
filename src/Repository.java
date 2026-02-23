import java.util.List;
import java.util.Optional;

public interface Repository<T> {
    void add(T item);
    boolean remove(T item);
    Optional<T> findById(String id);
    List<T> findAll();
    int count();
    void clear();
}