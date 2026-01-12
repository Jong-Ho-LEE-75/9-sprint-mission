package service.jcf;

import entity.User;
import service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JCFUserService implements UserService {
    private final Map<String, User> data;

    public JCFUserService() {
        this.data = new HashMap<>();
    }

    @Override
    public User create(User user) {
        data.put(user.getId(), user);
        return user;
    }

    @Override
    public User findById(String id) {
        return data.get(id);
    }

    @Override
    public List<User> findAll() {
        return data.values().stream().collect(Collectors.toList());
    }

    @Override
    public User update(String id, String username, String email, String nickname) {
        User user = data.get(id);
        if (user != null) {
            user.update(username, email, nickname);
        }
        return user;
    }

    @Override
    public void delete(String id) {
        data.remove(id);
    }
}