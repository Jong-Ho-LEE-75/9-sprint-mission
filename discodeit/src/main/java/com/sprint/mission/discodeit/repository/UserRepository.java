package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);  // 추가: 로그인 시 사용
    Optional<User> findByEmail(String email);        // 추가: 이메일 중복 체크
    List<User> findAll();
    void deleteById(UUID id);
    boolean existsById(UUID id);
    boolean existsByUsername(String username);       // 추가: username 중복 체크
    boolean existsByEmail(String email);             // 추가: email 중복 체크
}