package service.jcf;

import entity.User;
import service.UserService;
import java.util.*;

public class JCFUserService implements UserService {
    private final Map<UUID, User> userMap;

    public JCFUserService() {
        this.userMap = new HashMap<>();
    }

    @Override
    public User create(String userName, String email, String password) {
        User user = new User(userName, email, password);
        // user.getId()는 BaseEntity 생성자에서 만들어진 UUID
        userMap.put(user.getId(), user);
        return user;
    }

    @Override
    public User find(UUID id) {
        return userMap.get(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(userMap.values());
    }

    @Override
    public User update(UUID id, String username, String email, String password) {
        User user = userMap.get(id);
        if (user != null) {
            user.update(username, email, password);
        }
        return user;
    }

    @Override
    public void delete(UUID id) {
        userMap.remove(id);
    }
}