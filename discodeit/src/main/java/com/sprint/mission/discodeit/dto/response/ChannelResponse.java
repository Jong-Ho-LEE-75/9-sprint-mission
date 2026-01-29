package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.ChannelType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 채널 응답 DTO
 * ========================================
 *
 * 이 record는 채널 정보를 클라이언트에게 반환할 때 사용됩니다.
 *
 * [Channel Entity vs ChannelResponse]
 * Channel 엔티티:     id, type, name, description, createdAt, updatedAt
 * ChannelResponse DTO: id, type, name, description, createdAt, updatedAt,
 *                      + participantIds (PRIVATE 채널용)
 *                      + lastMessageAt (마지막 메시지 시간)
 *
 * [채널 타입별 필드 사용]
 * 1. PUBLIC 채널:
 *    - name, description: 채널 이름과 설명
 *    - participantIds: null (모든 사용자가 접근 가능하므로)
 *    - lastMessageAt: 마지막 메시지 시간 (채널 정렬에 사용)
 *
 * 2. PRIVATE 채널:
 *    - name, description: null (이름 없음)
 *    - participantIds: 참여자 ID 목록 (DM 참여자 표시용)
 *    - lastMessageAt: 마지막 메시지 시간
 *
 * [lastMessageAt의 용도]
 * 채널 목록을 정렬할 때 사용합니다.
 * 가장 최근에 메시지가 온 채널을 맨 위에 표시할 수 있습니다.
 *
 * [사용 예시]
 * // 채널 목록 표시 시
 * List<ChannelResponse> channels = channelService.findAllByUserId(userId);
 * for (ChannelResponse channel : channels) {
 *     if (channel.type() == ChannelType.PUBLIC) {
 *         System.out.println(channel.name());  // "공지사항"
 *     } else {
 *         // PRIVATE 채널은 참여자 이름을 표시
 *         System.out.println(channel.participantIds());  // [userId1, userId2]
 *     }
 * }
 *
 * @param id             채널 고유 식별자 (UUID)
 * @param type           채널 타입 (PUBLIC 또는 PRIVATE)
 * @param name           채널 이름 (PRIVATE면 null)
 * @param description    채널 설명 (PRIVATE면 null)
 * @param participantIds 참여자 ID 목록 (PUBLIC면 null, PRIVATE면 참여자 목록)
 * @param lastMessageAt  마지막 메시지 시간 (메시지가 없으면 null)
 * @param createdAt      채널 생성 시간
 * @param updatedAt      채널 수정 시간
 */
public record ChannelResponse(
        UUID id,
        ChannelType type,
        String name,
        String description,
        List<UUID> participantIds,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt
) {
}
