package com.sprint.mission.discodeit.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserService userService;

  private UserDto createTestUser(String username, String email) {
    UserCreateRequest request = new UserCreateRequest(username, email, "password123");
    return userService.create(request, Optional.empty());
  }

  @Test
  @DisplayName("유저 생성 통합 테스트")
  void createUser() throws Exception {
    MockMultipartFile requestPart = new MockMultipartFile(
        "userCreateRequest", "", MediaType.APPLICATION_JSON_VALUE,
        """
            {"username":"integrationUser","email":"int@test.com","password":"pw123"}
            """.getBytes());

    mockMvc.perform(multipart("/api/users")
            .file(requestPart))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("integrationUser"))
        .andExpect(jsonPath("$.email").value("int@test.com"));
  }

  @Test
  @DisplayName("전체 유저 조회 통합 테스트")
  void findAllUsers() throws Exception {
    createTestUser("user1", "u1@test.com");
    createTestUser("user2", "u2@test.com");

    mockMvc.perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("유저 수정 통합 테스트")
  void updateUser() throws Exception {
    UserDto user = createTestUser("oldname", "old@test.com");

    MockMultipartFile requestPart = new MockMultipartFile(
        "userUpdateRequest", "", MediaType.APPLICATION_JSON_VALUE,
        """
            {"newUsername":"newname","newEmail":"new@test.com","newPassword":null}
            """.getBytes());

    mockMvc.perform(multipart("/api/users/{userId}", user.id())
            .file(requestPart)
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("newname"))
        .andExpect(jsonPath("$.email").value("new@test.com"));
  }

  @Test
  @DisplayName("유저 삭제 통합 테스트")
  void deleteUser() throws Exception {
    UserDto user = createTestUser("deleteMe", "del@test.com");

    mockMvc.perform(delete("/api/users/{userId}", user.id()))
        .andExpect(status().isNoContent());
  }
}
