package com.sprint.mission.discodeit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.service.MessageService;
import java.time.Instant;
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

@WebMvcTest(MessageController.class)
class MessageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private MessageService messageService;

  @Test
  @DisplayName("메시지 생성 성공")
  void create_success() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    Instant now = Instant.now();

    MessageDto messageDto = new MessageDto(messageId, now, now, "hello", channelId,
        new UserDto(authorId, "user1", "u@test.com", null, true), List.of());
    given(messageService.create(any(), any())).willReturn(messageDto);

    MockMultipartFile requestPart = new MockMultipartFile(
        "messageCreateRequest", "", MediaType.APPLICATION_JSON_VALUE,
        """
            {"content":"hello","channelId":"%s","authorId":"%s"}
            """.formatted(channelId, authorId).getBytes());

    // when & then
    mockMvc.perform(multipart("/api/messages")
            .file(requestPart))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("hello"));
  }

  @Test
  @DisplayName("메시지 수정 성공")
  void update_success() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    Instant now = Instant.now();
    MessageDto messageDto = new MessageDto(messageId, now, now, "updated", UUID.randomUUID(),
        null, List.of());
    given(messageService.update(eq(messageId), any())).willReturn(messageDto);

    // when & then
    mockMvc.perform(patch("/api/messages/{messageId}", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"newContent":"updated"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("updated"));
  }

  @Test
  @DisplayName("메시지 수정 실패 - 메시지 없음")
  void update_fail_notFound() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    given(messageService.update(eq(messageId), any()))
        .willThrow(new NoSuchElementException("Message not found"));

    // when & then
    mockMvc.perform(patch("/api/messages/{messageId}", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"newContent":"updated"}
                """))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("메시지 삭제 성공")
  void delete_success() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    willDoNothing().given(messageService).delete(messageId);

    // when & then
    mockMvc.perform(delete("/api/messages/{messageId}", messageId))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("채널별 메시지 조회 성공")
  void findAllByChannelId_success() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();
    Instant now = Instant.now();
    MessageDto messageDto = new MessageDto(messageId, now, now, "hello", channelId, null,
        List.of());
    PageResponse<MessageDto> pageResponse = new PageResponse<>(List.of(messageDto), null, 50,
        false, null);

    given(messageService.findAllByChannelId(eq(channelId), any(), eq(50)))
        .willReturn(pageResponse);

    // when & then
    mockMvc.perform(get("/api/messages")
            .param("channelId", channelId.toString())
            .param("size", "50"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].content").value("hello"))
        .andExpect(jsonPath("$.hasNext").value(false));
  }
}
