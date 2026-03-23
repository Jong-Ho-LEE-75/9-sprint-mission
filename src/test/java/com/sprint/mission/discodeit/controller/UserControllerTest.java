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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserStatusService userStatusService;

    @Test
    @DisplayName("POST /api/users - 성공: 사용자를 생성한다")
    void create_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "testuser", "test@email.com", null, true);
        given(userService.create(any(), any(Optional.class))).willReturn(userDto);

        UserCreateRequest request = new UserCreateRequest("testuser", "test@email.com",
            "password1234");
        MockMultipartFile requestPart = new MockMultipartFile(
            "userCreateRequest", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/users").file(requestPart))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@email.com"));
    }

    @Test
    @DisplayName("GET /api/users - 성공: 사용자 목록을 조회한다")
    void findAll_success() throws Exception {
        UserDto userDto = new UserDto(UUID.randomUUID(), "testuser", "test@email.com", null, true);
        given(userService.findAll()).willReturn(List.of(userDto));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - 성공: 사용자를 삭제한다")
    void delete_success() throws Exception {
        UUID userId = UUID.randomUUID();
        willDoNothing().given(userService).delete(userId);

        mockMvc.perform(delete("/api/users/{userId}", userId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - 실패: 존재하지 않는 사용자")
    void delete_fail_notFound() throws Exception {
        UUID userId = UUID.randomUUID();
        willThrow(new UserNotFoundException(Map.of("userId", userId)))
            .given(userService).delete(userId);

        mockMvc.perform(delete("/api/users/{userId}", userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
