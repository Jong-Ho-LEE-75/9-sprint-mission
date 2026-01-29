package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.ChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.response.ChannelResponse;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 채널 서비스 인터페이스
 * ========================================
 *
 * 이 인터페이스는 채널 관련 비즈니스 로직을 정의합니다.
 * PUBLIC/PRIVATE 채널 생성, 조회, 수정, 삭제 기능을 제공합니다.
 *
 * [PUBLIC vs PRIVATE 채널]
 * PUBLIC 채널:
 * - 모든 사용자가 접근 가능
 * - 이름과 설명을 가짐
 * - 수정 가능
 *
 * PRIVATE 채널:
 * - 참여자만 접근 가능
 * - 이름과 설명 없음
 * - 수정 불가능
 * - 참여자는 ReadStatus로 관리
 *
 * [구현체]
 * BasicChannelService: 기본 채널 서비스 구현
 */
public interface ChannelService {
    /**
     * 공개(PUBLIC) 채널을 생성합니다.
     *
     * @param request 공개 채널 생성 요청 (name, description)
     * @return 생성된 채널 정보
     */
    ChannelResponse createPublic(PublicChannelCreateRequest request);

    /**
     * 비공개(PRIVATE) 채널을 생성합니다.
     *
     * [처리 과정]
     * 1. PRIVATE 타입 채널 생성 (name, description은 null)
     * 2. 각 참여자에 대한 ReadStatus 생성
     * 3. ChannelResponse 반환 (participantIds 포함)
     *
     * @param request 비공개 채널 생성 요청 (memberIds)
     * @return 생성된 채널 정보
     */
    ChannelResponse createPrivate(PrivateChannelCreateRequest request);

    /**
     * ID로 채널을 조회합니다.
     *
     * @param id 채널 ID
     * @return 조회된 채널 정보
     * @throws java.util.NoSuchElementException 채널이 없을 경우
     */
    ChannelResponse find(UUID id);

    /**
     * 특정 사용자가 접근 가능한 모든 채널을 조회합니다.
     *
     * [조회 기준]
     * - PUBLIC 채널: 모든 사용자가 접근 가능
     * - PRIVATE 채널: 해당 사용자가 참여 중인 것만 (ReadStatus 존재)
     *
     * @param userId 사용자 ID
     * @return 사용자가 접근 가능한 채널 목록
     */
    List<ChannelResponse> findAllByUserId(UUID userId);

    /**
     * 채널 정보를 수정합니다.
     *
     * [주의] PUBLIC 채널만 수정 가능합니다.
     * PRIVATE 채널 수정 시도 시 예외가 발생합니다.
     *
     * @param id      수정할 채널 ID
     * @param request 수정 요청 (name, description)
     * @return 수정된 채널 정보
     * @throws java.util.NoSuchElementException 채널이 없을 경우
     * @throws IllegalArgumentException         PRIVATE 채널을 수정하려 할 경우
     */
    ChannelResponse update(UUID id, ChannelUpdateRequest request);

    /**
     * 채널을 삭제합니다.
     *
     * [처리 과정 (Cascading Delete)]
     * 1. 채널의 모든 메시지의 첨부파일 삭제
     * 2. 채널의 모든 메시지 삭제
     * 3. 채널의 모든 ReadStatus 삭제
     * 4. 채널 삭제
     *
     * @param id 삭제할 채널 ID
     * @throws java.util.NoSuchElementException 채널이 없을 경우
     */
    void delete(UUID id);
}
