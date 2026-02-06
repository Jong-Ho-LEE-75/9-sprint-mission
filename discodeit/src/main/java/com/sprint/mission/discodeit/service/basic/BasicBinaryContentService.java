package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ========================================
 * BinaryContentService 인터페이스의 기본 구현체
 * ========================================
 *
 * 이 클래스는 바이너리 콘텐츠(파일) 관련 비즈니스 로직을 구현합니다.
 * 파일 업로드, 조회, 삭제 기능을 제공합니다.
 *
 * [단순한 구현]
 * BinaryContent는 불변 객체이고, 복잡한 비즈니스 로직이 없어서
 * Repository 호출을 단순히 위임하는 형태입니다.
 *
 * [왜 Service를 거치나요?]
 * 지금은 단순하지만, 나중에 다음과 같은 로직이 추가될 수 있습니다:
 * - 파일 크기 검증
 * - 허용된 파일 형식 검증
 * - 바이러스 검사
 * - 이미지 리사이징
 * - 클라우드 스토리지 연동
 *
 * [어노테이션 설명]
 * @Service: Spring Bean으로 등록
 * @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
 */
@Service
@RequiredArgsConstructor
public class BasicBinaryContentService implements BinaryContentService {
    /**
     * 바이너리 콘텐츠를 저장하고 조회하는 리포지토리
     */
    private final BinaryContentRepository binaryContentRepository;

    /**
     * ========================================
     * 바이너리 콘텐츠(파일)를 생성합니다.
     * ========================================
     *
     * Request DTO를 Entity로 변환하여 저장합니다.
     *
     * [코드 설명]
     * 1. new BinaryContent(...): DTO의 데이터로 새 엔티티 객체 생성
     * 2. request.fileName(): record의 필드 접근 메서드 (getter와 동일)
     * 3. binaryContentRepository.save(): 엔티티를 저장소에 저장
     *
     * [record의 필드 접근]
     * record는 getter 대신 필드명과 동일한 메서드를 사용합니다.
     * - 일반 클래스: request.getFileName()
     * - record: request.fileName()
     */
    @Override
    public BinaryContent create(BinaryContentCreateRequest request) {
        // DTO에서 데이터를 꺼내 Entity 객체 생성
        BinaryContent binaryContent = new BinaryContent(
                request.fileName(),     // 파일명
                request.contentType(),  // MIME 타입 (예: "image/png")
                request.data()          // 파일 데이터 (byte[])
        );
        // 저장소에 저장하고 저장된 엔티티 반환
        return binaryContentRepository.save(binaryContent);
    }

    /**
     * ========================================
     * ID로 바이너리 콘텐츠를 조회합니다.
     * ========================================
     *
     * [코드 상세 분석]
     * binaryContentRepository.findById(id)
     *     .orElseThrow(() -> new NoSuchElementException("BinaryContent not found: " + id));
     *
     * 이 코드는 세 부분으로 나눌 수 있습니다:
     *
     * ----------------------------------------
     * 1단계: findById(id) - Optional 반환
     * ----------------------------------------
     * findById()는 Optional<BinaryContent>를 반환합니다.
     *
     * [Optional이란?]
     * "값이 있을 수도 있고, 없을 수도 있다"를 표현하는 컨테이너입니다.
     * null을 직접 다루는 것보다 안전합니다.
     *
     * Optional의 두 가지 상태:
     * - Optional.of(값): 값이 있는 상태
     * - Optional.empty(): 값이 없는 상태 (null 대신 사용)
     *
     * [왜 null 대신 Optional을 사용하나요?]
     * // null 사용 시 - NullPointerException 위험!
     * BinaryContent bc = repository.findById(id);  // null일 수 있음
     * bc.getFileName();  // null이면 NullPointerException 발생!
     *
     * // Optional 사용 시 - 컴파일러가 null 체크를 강제함
     * Optional<BinaryContent> opt = repository.findById(id);
     * // opt.getFileName(); ← 이렇게 직접 접근 불가능!
     * // 반드시 값 존재 여부를 먼저 확인해야 함
     *
     * ----------------------------------------
     * 2단계: .orElseThrow(...) - 값 추출 또는 예외
     * ----------------------------------------
     * Optional에서 값을 꺼내는 메서드입니다.
     *
     * [orElseThrow의 동작]
     * - Optional에 값이 있으면: 그 값을 반환
     * - Optional이 비어있으면: 괄호 안의 예외를 발생시킴
     *
     * [Optional의 다양한 값 추출 방법]
     * // 1. orElseThrow: 없으면 예외 발생 (가장 많이 사용)
     * BinaryContent bc = opt.orElseThrow(() -> new 예외());
     *
     * // 2. orElse: 없으면 기본값 반환
     * BinaryContent bc = opt.orElse(defaultValue);
     *
     * // 3. orElseGet: 없으면 람다로 기본값 생성
     * BinaryContent bc = opt.orElseGet(() -> createDefault());
     *
     * // 4. ifPresent: 있을 때만 동작 수행
     * opt.ifPresent(bc -> System.out.println(bc.getFileName()));
     *
     * ----------------------------------------
     * 3단계: () -> new NoSuchElementException(...)
     * ----------------------------------------
     * 이것은 "람다 표현식(Lambda Expression)"입니다.
     *
     * [람다 표현식이란?]
     * 익명 함수(이름 없는 함수)를 간결하게 표현하는 방법입니다.
     * Java 8부터 도입되었습니다.
     *
     * [람다 표현식의 구조]
     * (매개변수) -> { 실행할 코드 }
     *
     * [람다 예시]
     * () -> new Exception()           // 매개변수 없음, 예외 객체 생성
     * (x) -> x * 2                    // x를 받아서 2배 반환
     * (a, b) -> a + b                 // a, b를 받아서 합계 반환
     * (user) -> user.getName()        // user를 받아서 이름 반환
     *
     * [왜 람다를 사용하나요?]
     * orElseThrow가 예외 객체를 "필요할 때만" 생성하기 위해서입니다.
     *
     * // 람다 없이 (비효율적)
     * NoSuchElementException ex = new NoSuchElementException("...");  // 항상 생성됨
     * opt.orElseThrow(ex);  // 값이 있어도 예외 객체가 이미 만들어짐
     *
     * // 람다 사용 (효율적)
     * opt.orElseThrow(() -> new NoSuchElementException("..."));
     * // 값이 있으면 람다가 실행되지 않음 → 예외 객체 생성 안 됨
     * // 값이 없을 때만 람다 실행 → 예외 객체 생성
     *
     * ----------------------------------------
     * 전체 코드 해석
     * ----------------------------------------
     * 1. findById(id): ID로 데이터 조회 → Optional 반환
     * 2. .orElseThrow(...): Optional에 값이 있으면 그 값 반환
     * 3. 값이 없으면: NoSuchElementException 발생
     *
     * [실행 흐름 예시]
     * // 데이터가 존재하는 경우
     * findById("abc") → Optional.of(BinaryContent객체)
     * .orElseThrow(...) → BinaryContent객체 반환 (예외 발생 안 함)
     *
     * // 데이터가 없는 경우
     * findById("xyz") → Optional.empty()
     * .orElseThrow(...) → NoSuchElementException 발생!
     *
     * @param id 조회할 바이너리 콘텐츠 ID
     * @return 조회된 BinaryContent 엔티티
     * @throws NoSuchElementException 해당 ID의 콘텐츠가 없을 경우
     */
    @Override
    public BinaryContent find(UUID id) {
        return binaryContentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("BinaryContent not found: " + id));
    }

    /**
     * ========================================
     * 여러 ID로 바이너리 콘텐츠 목록을 조회합니다.
     * ========================================
     *
     * 메시지의 첨부파일 목록을 한 번에 조회할 때 유용합니다.
     *
     * [사용 예시]
     * // 메시지에 첨부된 파일 3개의 ID가 있다고 가정
     * List<UUID> attachmentIds = List.of(id1, id2, id3);
     *
     * // 한 번의 호출로 3개 파일 모두 조회
     * List<BinaryContent> files = binaryContentService.findAllByIdIn(attachmentIds);
     *
     * [왜 findById를 3번 호출하지 않나요?]
     * 효율성 때문입니다.
     * - findById 3번: DB/저장소 접근 3번
     * - findAllByIdIn 1번: DB/저장소 접근 1번
     *
     * @param ids 조회할 바이너리 콘텐츠 ID 목록
     * @return 조회된 BinaryContent 목록 (없는 ID는 제외됨)
     */
    @Override
    public List<BinaryContent> findAllByIdIn(List<UUID> ids) {
        return binaryContentRepository.findAllByIdIn(ids);
    }

    /**
     * ========================================
     * 바이너리 콘텐츠를 삭제합니다.
     * ========================================
     *
     * [주의사항]
     * 이 파일을 참조하는 곳(User의 profileId, Message의 attachmentIds)에서
     * 먼저 참조를 제거한 후 삭제해야 합니다.
     * 그렇지 않으면 "고아 참조(Orphan Reference)"가 발생합니다.
     *
     * [고아 참조란?]
     * 삭제된 데이터를 가리키는 ID가 남아있는 상태입니다.
     * 예: User.profileId가 "abc"인데, "abc" 파일은 이미 삭제됨
     * → profileId로 조회하면 NoSuchElementException 발생!
     *
     * @param id 삭제할 바이너리 콘텐츠 ID
     */
    @Override
    public void delete(UUID id) {
        binaryContentRepository.deleteById(id);
    }
}
