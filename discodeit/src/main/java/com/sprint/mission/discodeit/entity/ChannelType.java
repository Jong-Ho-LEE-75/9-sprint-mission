package com.sprint.mission.discodeit.entity;

/**
 * ========================================
 * 채널 타입을 정의하는 열거형 (Enum)
 * ========================================
 *
 * 이 열거형은 채널이 가질 수 있는 두 가지 타입을 정의합니다.
 *
 * [열거형(Enum)이란?]
 * 고정된 상수 집합을 정의하는 특별한 클래스입니다.
 * 예를 들어, 요일(MON, TUE, ...)이나 계절(SPRING, SUMMER, ...)처럼
 * 정해진 값들 중 하나만 사용해야 할 때 열거형을 사용합니다.
 *
 * [왜 String 대신 Enum을 사용하나요?]
 * 1. 타입 안전성: "PUBLIC"을 "PUBILC"으로 오타를 내도 컴파일러가 잡지 못함
 *    → Enum을 사용하면 ChannelType.PUBILC 같은 오타는 컴파일 에러 발생
 * 2. 코드 가독성: channel.getType() == ChannelType.PUBLIC 이 더 명확함
 * 3. IDE 지원: 자동완성으로 가능한 값들을 쉽게 확인 가능
 *
 * [사용 예시]
 * Channel channel = new Channel(ChannelType.PUBLIC, "공지", "공지사항 채널");
 * if (channel.getType() == ChannelType.PRIVATE) {
 *     // 비공개 채널 처리
 * }
 */
public enum ChannelType {
    /**
     * 공개 채널 (PUBLIC Channel)
     *
     * 모든 사용자가 접근할 수 있는 채널입니다.
     * Discord의 서버 내 공개 채널과 유사합니다.
     *
     * [특징]
     * - 모든 사용자가 메시지를 읽고 쓸 수 있음
     * - 채널 이름과 설명을 가짐
     * - 채널 이름과 설명을 수정할 수 있음
     *
     * [사용 예시]
     * - 공지사항 채널
     * - 일반 대화 채널
     * - 질문 채널
     */
    PUBLIC,

    /**
     * 비공개 채널 (PRIVATE Channel)
     *
     * 초대된 사용자만 접근할 수 있는 비공개 채널입니다.
     * Discord의 DM(Direct Message)과 유사합니다.
     *
     * [특징]
     * - 참여자 목록에 있는 사용자만 메시지를 읽고 쓸 수 있음
     * - 채널 이름과 설명이 없음 (null)
     * - 수정이 불가능함
     * - 채널 생성 시 각 참여자에 대한 ReadStatus가 자동으로 생성됨
     *
     * [사용 예시]
     * - 1:1 대화
     * - 그룹 비밀 대화
     */
    PRIVATE
}
