package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.service.ReadStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 읽기 상태 관련 REST API 컨트롤러
 *
 * 사용자의 채널별 메시지 읽기 상태를 관리하는 기능을 제공합니다.
 */
@RestController
@RequestMapping("/read-statuses")
@RequiredArgsConstructor
public class ReadStatusController {

	private final ReadStatusService readStatusService;

	/**
	 * 메시지 수신 정보 생성
	 * POST /read-statuses
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<ReadStatus> createReadStatus(@RequestBody ReadStatusCreateRequest request) {
		ReadStatus readStatus = readStatusService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(readStatus);
	}

	/**
	 * 특정 사용자의 메시지 수신 정보 조회
	 * GET /read-statuses?userId={userId}
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<ReadStatus>> getReadStatusesByUser(@RequestParam UUID userId) {
		List<ReadStatus> readStatuses = readStatusService.findAllByUserId(userId);
		return ResponseEntity.ok(readStatuses);
	}

	/**
	 * 메시지 수신 정보 수정
	 * PUT /read-statuses/{id}
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public ResponseEntity<ReadStatus> updateReadStatus(
			@PathVariable UUID id,
			@RequestBody ReadStatusUpdateRequest request
	) {
		ReadStatus readStatus = readStatusService.update(id, request);
		return ResponseEntity.ok(readStatus);
	}
}
