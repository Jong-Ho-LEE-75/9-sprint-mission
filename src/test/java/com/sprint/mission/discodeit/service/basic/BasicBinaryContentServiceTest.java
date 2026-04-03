package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.mapper.BinaryContentMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicBinaryContentServiceTest {

  @Mock
  private BinaryContentRepository binaryContentRepository;
  @Mock
  private BinaryContentMapper binaryContentMapper;

  @InjectMocks
  private BasicBinaryContentService binaryContentService;

  private final UUID contentId = UUID.randomUUID();

  @Test
  @DisplayName("파일 단건 조회 성공")
  void find_success() {
    // given
    BinaryContent binaryContent = new BinaryContent("test.png", 1024L, "image/png");
    BinaryContentDto expectedDto = new BinaryContentDto(contentId, "test.png", 1024L, "image/png");

    given(binaryContentRepository.findById(contentId)).willReturn(Optional.of(binaryContent));
    given(binaryContentMapper.toDto(binaryContent)).willReturn(expectedDto);

    // when
    BinaryContentDto result = binaryContentService.find(contentId);

    // then
    assertThat(result.id()).isEqualTo(contentId);
  }

  @Test
  @DisplayName("파일 단건 조회 실패 - 없음")
  void find_fail_notFound() {
    // given
    given(binaryContentRepository.findById(contentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> binaryContentService.find(contentId))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("ID 목록으로 파일 조회")
  void findAllByIdIn_success() {
    // given
    List<UUID> ids = List.of(contentId);
    BinaryContent binaryContent = new BinaryContent("test.png", 1024L, "image/png");
    BinaryContentDto dto = new BinaryContentDto(contentId, "test.png", 1024L, "image/png");

    given(binaryContentRepository.findAllByIdIn(ids)).willReturn(List.of(binaryContent));
    given(binaryContentMapper.toDto(binaryContent)).willReturn(dto);

    // when
    List<BinaryContentDto> result = binaryContentService.findAllByIdIn(ids);

    // then
    assertThat(result).hasSize(1);
  }
}
