package com.sprint.mission.discodeit.dto.request;

/**
 * 채널 정보 수정 요청 DTO
 * 채널의 이름과 설명을 변경할 때 사용합니다.
 *
 * @param name 변경할 채널 이름
 * @param description 변경할 채널 설명
 */
public record ChannelUpdateRequest(
        String name,
        String description
) {
}
