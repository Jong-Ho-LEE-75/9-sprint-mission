package com.sprint.mission.discodeit.dto.request;

import java.time.Instant;
import java.util.UUID;

/**
 * ========================================
 * 읽기 상태 생성 요청 DTO
 * ========================================
 *
 * 이 record는 새 읽기 상태 생성 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [ReadStatus가 생성되는 시점]
 * - PRIVATE 채널 생성 시: 각 참여자에 대해 자동으로 생성 (ChannelService에서 처리)
 * - 사용자가 PUBLIC 채널에 처음 접근할 때 (구현에 따라 다름)
 *
 * [주의사항]
 * 같은 userId와 channelId 조합의 ReadStatus는 중복 생성할 수 없습니다.
 * 중복 시도 시 IllegalArgumentException이 발생합니다.
 *
 * [사용 예시]
 * ReadStatusCreateRequest request = new ReadStatusCreateRequest(
 *     userId,
 *     channelId,
 *     Instant.now()  // 현재 시간을 마지막 읽은 시간으로 설정
 * );
 * ReadStatus readStatus = readStatusService.create(request);
 *
 * @param userId     사용자 ID - 이 사용자의 읽기 상태
 * @param channelId  채널 ID - 이 채널에 대한 읽기 상태
 * @param lastReadAt 마지막으로 읽은 시간
 */
public record ReadStatusCreateRequest(
        UUID userId,
        UUID channelId,
        Instant lastReadAt
) {
}
