package service;

import entity.User;
import java.util.List;
import java.util.UUID;

public interface UserSerive {
    // create
    User create(String userName, String email, String status);

    // read
    User findById(UUID id);

    // read total
    List<User> findAll();

    // update
    User update(UUID id, String userName, String email, String status);

    // delete

    void delete(UUID id);


    //

}
