package service;

import entity.User;
import java.util.List;

public interface UserService {
    User create(User user);
    User findById(String id);
    List<User> findAll();
    User update(String id, String username, String email, String nickname);
    void delete(String id);
}