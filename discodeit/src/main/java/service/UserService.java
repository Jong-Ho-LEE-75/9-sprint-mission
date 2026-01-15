package service;

import entity.User;
import java.util.List;
import java.util.UUID;

public interface UserService {
    User create(String userName, String email, String password);
    User find(UUID id); // Long -> UUID
    List<User> findAll();
    User update(UUID id, String userName, String email, String password);
    void delete(UUID id); // Long -> UUID
}