package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

	private final MessageService messageService;

	/**
	 * POST /api/messages
	 * multipart/form-data: messageCreateRequest(JSON part) + attachments(binary, optional)
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Message> create(
			@RequestPart("messageCreateRequest") MessageCreateRequest messageCreateRequest,
			@RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
	) throws IOException {
		List<BinaryContentCreateRequest> attachmentRequests = resolveAttachments(attachments);
		Message message = messageService.create(messageCreateRequest, attachmentRequests);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	/**
	 * GET /api/messages?channelId={channelId}
	 */
	@GetMapping
	public ResponseEntity<List<Message>> findAllByChannelId(@RequestParam UUID channelId) {
		List<Message> messages = messageService.findAllByChannelId(channelId);
		return ResponseEntity.ok(messages);
	}

	/**
	 * PATCH /api/messages/{messageId}
	 */
	@PatchMapping("/{messageId}")
	public ResponseEntity<Message> update(
			@PathVariable UUID messageId,
			@RequestBody MessageUpdateRequest request
	) {
		Message message = messageService.update(messageId, request);
		return ResponseEntity.ok(message);
	}

	/**
	 * DELETE /api/messages/{messageId}
	 */
	@DeleteMapping("/{messageId}")
	public ResponseEntity<Void> delete(@PathVariable UUID messageId) {
		messageService.delete(messageId);
		return ResponseEntity.noContent().build();
	}

	private List<BinaryContentCreateRequest> resolveAttachments(List<MultipartFile> attachments) throws IOException {
		if (attachments == null || attachments.isEmpty()) {
			return new ArrayList<>();
		}
		List<BinaryContentCreateRequest> result = new ArrayList<>();
		for (MultipartFile file : attachments) {
			if (!file.isEmpty()) {
				result.add(new BinaryContentCreateRequest(
						file.getOriginalFilename(),
						file.getContentType(),
						file.getBytes()
				));
			}
		}
		return result;
	}
}
