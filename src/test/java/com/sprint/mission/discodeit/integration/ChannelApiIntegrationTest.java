package com.sprint.mission.discodeit.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.UserService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChannelApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ChannelService channelService;

  @Autowired
  private UserService userService;

  private static final AtomicInteger counter = new AtomicInteger(0);

  private UserDto createUniqueUser() {
    int i = counter.incrementAndGet();
    UserCreateRequest request = new UserCreateRequest("user" + i, "user" + i + "@test.com", "pw");
    return userService.create(request, Optional.empty());
  }

  @Test
  @DisplayName("공개 채널 생성 통합 테스트")
  void createPublicChannel() throws Exception {
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
  @DisplayName("비공개 채널 생성 통합 테스트")
  void createPrivateChannel() throws Exception {
    UserDto user1 = createUniqueUser();
    UserDto user2 = createUniqueUser();

    mockMvc.perform(post("/api/channels/private")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"participantIds":["%s","%s"]}
                """.formatted(user1.id(), user2.id())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("PRIVATE"));
  }

  @Test
  @DisplayName("채널 수정 통합 테스트")
  void updateChannel() throws Exception {
    ChannelDto channel = channelService.create(new PublicChannelCreateRequest("old", "old desc"));

    mockMvc.perform(patch("/api/channels/{channelId}", channel.id())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"newName":"updated","newDescription":"updated desc"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updated"));
  }

  @Test
  @DisplayName("유저별 채널 조회 통합 테스트")
  void findAllByUserId() throws Exception {
    UserDto user = createUniqueUser();
    channelService.create(new PublicChannelCreateRequest("general", "desc"));

    mockMvc.perform(get("/api/channels")
            .param("userId", user.id().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @DisplayName("채널 삭제 통합 테스트")
  void deleteChannel() throws Exception {
    ChannelDto channel = channelService.create(new PublicChannelCreateRequest("toDelete", "desc"));

    mockMvc.perform(delete("/api/channels/{channelId}", channel.id()))
        .andExpect(status().isNoContent());
  }
}
