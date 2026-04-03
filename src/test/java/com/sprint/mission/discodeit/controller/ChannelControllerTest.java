package com.sprint.mission.discodeit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.service.ChannelService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChannelController.class)
class ChannelControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ChannelService channelService;

  @Test
  @DisplayName("공개 채널 생성 성공")
  void createPublic_success() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    ChannelDto channelDto = new ChannelDto(channelId, ChannelType.PUBLIC, "general", "일반 채널",
        List.of(), null);
    given(channelService.create(any(com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest.class)))
        .willReturn(channelDto);

    // when & then
    mockMvc.perform(post("/api/channels/public")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"general","description":"일반 채널"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("general"))
        .andExpect(jsonPath("$.type").value("PUBLIC"));
  }

  @Test
  @DisplayName("비공개 채널 생성 성공")
  void createPrivate_success() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    ChannelDto channelDto = new ChannelDto(channelId, ChannelType.PRIVATE, null, null,
        List.of(), null);
    given(channelService.create(any(com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest.class)))
        .willReturn(channelDto);

    UUID userId = UUID.randomUUID();

    // when & then
    mockMvc.perform(post("/api/channels/private")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"participantIds":["%s"]}
                """.formatted(userId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("PRIVATE"));
  }

  @Test
  @DisplayName("채널 목록 조회 성공")
  void findAll_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID channelId = UUID.randomUUID();
    ChannelDto channelDto = new ChannelDto(channelId, ChannelType.PUBLIC, "general", "desc",
        List.of(), null);
    given(channelService.findAllByUserId(userId)).willReturn(List.of(channelDto));

    // when & then
    mockMvc.perform(get("/api/channels")
            .param("userId", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("general"));
  }

  @Test
  @DisplayName("채널 수정 실패 - 채널 없음")
  void update_fail_notFound() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    given(channelService.update(eq(channelId), any()))
        .willThrow(new NoSuchElementException("Channel not found"));

    // when & then
    mockMvc.perform(patch("/api/channels/{channelId}", channelId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"newName":"updated","newDescription":"updated desc"}
                """))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("채널 삭제 성공")
  void delete_success() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    willDoNothing().given(channelService).delete(channelId);

    // when & then
    mockMvc.perform(delete("/api/channels/{channelId}", channelId))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("채널 수정 실패 - PRIVATE 채널")
  void update_fail_privateChannel() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    given(channelService.update(eq(channelId), any()))
        .willThrow(new IllegalArgumentException("Private channel cannot be updated"));

    // when & then
    mockMvc.perform(patch("/api/channels/{channelId}", channelId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"newName":"updated","newDescription":"updated desc"}
                """))
        .andExpect(status().isBadRequest());
  }
}
