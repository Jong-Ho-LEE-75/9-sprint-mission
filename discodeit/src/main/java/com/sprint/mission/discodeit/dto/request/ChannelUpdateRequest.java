package com.sprint.mission.discodeit.dto.request;

/**
 * ========================================
 * 채널 정보 수정 요청 DTO
 * ========================================
 *
 * 이 record는 채널 정보 수정 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [주의사항]
 * PUBLIC 채널만 수정 가능합니다!
 * PRIVATE 채널을 수정하려고 하면 IllegalArgumentException이 발생합니다.
 *
 * [부분 업데이트 지원]
 * 변경하고 싶은 필드만 값을 넣고, 나머지는 null로 전달하면 됩니다.
 *
 * [사용 예시]
 * // 이름만 변경
 * ChannelUpdateRequest request = new ChannelUpdateRequest("새 채널명", null);
 *
 * // 설명만 변경
 * ChannelUpdateRequest request = new ChannelUpdateRequest(null, "새로운 설명입니다");
 *
 * // 둘 다 변경
 * ChannelUpdateRequest request = new ChannelUpdateRequest("새 채널명", "새 설명");
 *
 * channelService.update(channelId, request);
 *
 * @param name        변경할 채널 이름 (null이면 변경하지 않음)
 * @param description 변경할 채널 설명 (null이면 변경하지 않음)
 */
public record ChannelUpdateRequest(
        String name,
        String description
) {
}
