package com.sprint.mission.discodeit.dto.request;

/**
 * ========================================
 * 공개 채널(PUBLIC Channel) 생성 요청 DTO
 * ========================================
 *
 * 이 record는 공개 채널 생성 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [PUBLIC 채널의 특징]
 * - 모든 사용자가 접근 가능
 * - 이름과 설명을 가짐
 * - 이름과 설명 수정 가능
 *
 * [사용 예시]
 * PublicChannelCreateRequest request = new PublicChannelCreateRequest(
 *     "공지사항",
 *     "중요 공지를 올리는 채널입니다"
 * );
 * ChannelResponse channel = channelService.createPublic(request);
 *
 * @param name        채널 이름 (예: "공지사항", "일반 대화", "코드 리뷰")
 * @param description 채널 설명 (예: "자유롭게 대화하세요")
 */
public record PublicChannelCreateRequest(
        String name,
        String description
) {
}
