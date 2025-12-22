package generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Basit generic repository: id -> T saklar.
public class Repository<T> {
    private final Map<Integer, T> store = new HashMap<>();

    public void save(int id, T entity) {
        store.put(id, entity);
    }

    public T findById(int id) {
        return store.get(id);
    }

    public boolean existsById(int id) {
        return store.containsKey(id);
    }

    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    public void deleteById(int id) {
        store.remove(id);
    }

    public int size() {
        return store.size();
    }
}
