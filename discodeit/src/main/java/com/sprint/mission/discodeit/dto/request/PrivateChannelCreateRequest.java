package com.sprint.mission.discodeit.dto.request;

import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 비공개 채널(PRIVATE Channel) 생성 요청 DTO
 * ========================================
 *
 * 이 record는 비공개 채널(DM) 생성 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [PRIVATE 채널의 특징]
 * - 초대된 사용자(memberIds에 포함된 사용자)만 접근 가능
 * - 이름과 설명이 없음 (Discord DM처럼)
 * - 수정 불가능
 * - 생성 시 각 참여자에 대한 ReadStatus가 자동 생성됨
 *
 * [참여자 관리]
 * PRIVATE 채널의 참여자는 ReadStatus 엔티티를 통해 관리됩니다.
 * ReadStatus가 있는 사용자만 해당 채널에 접근할 수 있습니다.
 *
 * [사용 예시]
 * // 1:1 대화방 생성
 * PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(
 *     List.of(userId1, userId2)
 * );
 *
 * // 그룹 대화방 생성 (3명 이상)
 * PrivateChannelCreateRequest groupRequest = new PrivateChannelCreateRequest(
 *     List.of(userId1, userId2, userId3, userId4)
 * );
 *
 * ChannelResponse channel = channelService.createPrivate(request);
 *
 * @param memberIds 채널에 참여할 사용자들의 ID 목록
 */
public record PrivateChannelCreateRequest(
        List<UUID> memberIds
) {
}
