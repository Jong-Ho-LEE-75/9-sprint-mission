package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageResponse;
import com.sprint.mission.discodeit.service.MessageService;
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
 * 메시지 관련 REST API 컨트롤러
 *
 * 메시지 전송, 조회, 수정, 삭제 및 첨부파일 관리 기능을 제공합니다.
 */
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

	private final MessageService messageService;

	/**
	 * 메시지 전송
	 * POST /messages
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<MessageResponse> createMessage(
			@RequestBody MessageCreateRequest messageRequest,
			@RequestBody(required = false) List<BinaryContentCreateRequest> attachmentRequests
	) {
		MessageResponse message = messageService.create(messageRequest, attachmentRequests);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	/**
	 * 특정 채널의 메시지 목록 조회
	 * GET /messages?channelId={channelId}
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<MessageResponse>> getMessagesByChannel(@RequestParam UUID channelId) {
		List<MessageResponse> messages = messageService.findAllByChannelId(channelId);
		return ResponseEntity.ok(messages);
	}

	/**
	 * 특정 메시지 조회
	 * GET /messages/{id}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public ResponseEntity<MessageResponse> getMessage(@PathVariable UUID id) {
		MessageResponse message = messageService.find(id);
		return ResponseEntity.ok(message);
	}

	/**
	 * 메시지 수정
	 * PUT /messages/{id}
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public ResponseEntity<MessageResponse> updateMessage(
			@PathVariable UUID id,
			@RequestBody MessageUpdateRequest request
	) {
		MessageResponse message = messageService.update(id, request);
		return ResponseEntity.ok(message);
	}

	/**
	 * 메시지 삭제
	 * DELETE /messages/{id}
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public ResponseEntity<Void> deleteMessage(@PathVariable UUID id) {
		messageService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
