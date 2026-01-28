package com.sprint.mission.discodeit.entity;

import lombok.Getter;

@Getter
public class User extends BaseEntity {
    private String username;
    private String email;
    private String password;

    public User(String username, String email, String password) {
        super();
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public void update(String username, String email, String password) {
        if (username != null) this.username = username;
        if (email != null) this.email = email;
        if (password != null) this.password = password;
        updateTimeStamp();
    }
}