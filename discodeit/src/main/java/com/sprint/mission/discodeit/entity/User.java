package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.util.UUID;

@Getter
public class User extends BaseEntity {
    private String username;
    private String email;
    private String password;
    private UUID profileId;    // 프로필 이미지 ID (BinaryContent 참조, nullable)

    public User(String username, String email, String password, UUID profileId) {
        super();
        this.username = username;
        this.email = email;
        this.password = password;
        this.profileId = profileId;
    }

    public void update(String username, String email, String password, UUID profileId) {
        if (username != null) this.username = username;
        if (email != null) this.email = email;
        if (password != null) this.password = password;
        if (profileId != null) this.profileId = profileId;
        updateTimeStamp();
    }
}