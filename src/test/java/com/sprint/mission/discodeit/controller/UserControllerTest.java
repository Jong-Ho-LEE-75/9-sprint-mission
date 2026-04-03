package com.sprint.mission.discodeit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @MockBean
  private UserStatusService userStatusService;

  @Test
  @DisplayName("유저 생성 성공")
  void create_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(userId, "testuser", "test@test.com", null, true);
    given(userService.create(any(), any())).willReturn(userDto);

    MockMultipartFile requestPart = new MockMultipartFile(
        "userCreateRequest", "", MediaType.APPLICATION_JSON_VALUE,
        """
            {"username":"testuser","email":"test@test.com","password":"password123"}
            """.getBytes());

    // when & then
    mockMvc.perform(multipart("/api/users")
            .file(requestPart))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Test
  @DisplayName("유저 생성 실패 - 이메일 중복")
  void create_fail_duplicateEmail() throws Exception {
    // given
    given(userService.create(any(), any()))
        .willThrow(new IllegalArgumentException("User with email dup@test.com already exists"));

    MockMultipartFile requestPart = new MockMultipartFile(
        "userCreateRequest", "", MediaType.APPLICATION_JSON_VALUE,
        """
            {"username":"testuser","email":"dup@test.com","password":"password123"}
            """.getBytes());

    // when & then
    mockMvc.perform(multipart("/api/users")
            .file(requestPart))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("전체 유저 조회 성공")
  void findAll_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(userId, "testuser", "test@test.com", null, true);
    given(userService.findAll()).willReturn(List.of(userDto));

    // when & then
    mockMvc.perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("testuser"));
  }

  @Test
  @DisplayName("유저 삭제 성공")
  void delete_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    willDoNothing().given(userService).delete(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("유저 삭제 실패 - 유저 없음")
  void delete_fail_notFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    willThrow(new NoSuchElementException("User not found"))
        .given(userService).delete(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isNotFound());
  }
}
