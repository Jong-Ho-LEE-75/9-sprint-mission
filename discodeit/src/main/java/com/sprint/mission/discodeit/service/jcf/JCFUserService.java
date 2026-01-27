package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.entity.User;

import java.util.*;

public class JCFUserService implements UserService {
    private final Map<UUID, User> userMap;

    public JCFUserService() {
        this.userMap = new HashMap<>();
    }

    @Override
    public User create(String username, String email, String password) {
        User user = new User(username, email, password);
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