package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.ChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.response.ChannelResponse;
import com.sprint.mission.discodeit.service.ChannelService;
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
 * 채널 관련 REST API 컨트롤러
 *
 * 공개/비공개 채널 생성, 조회, 수정, 삭제 기능을 제공합니다.
 */
@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {

	private final ChannelService channelService;

	/**
	 * 공개 채널 생성
	 * POST /channels/public
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/public")
	public ResponseEntity<ChannelResponse> createPublicChannel(@RequestBody PublicChannelCreateRequest request) {
		ChannelResponse channel = channelService.createPublic(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(channel);
	}

	/**
	 * 비공개 채널 생성
	 * POST /channels/private
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/private")
	public ResponseEntity<ChannelResponse> createPrivateChannel(@RequestBody PrivateChannelCreateRequest request) {
		ChannelResponse channel = channelService.createPrivate(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(channel);
	}

	/**
	 * 특정 사용자가 볼 수 있는 모든 채널 조회
	 * GET /channels?userId={userId}
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<ChannelResponse>> getChannelsByUser(@RequestParam UUID userId) {
		List<ChannelResponse> channels = channelService.findAllByUserId(userId);
		return ResponseEntity.ok(channels);
	}

	/**
	 * 특정 채널 조회
	 * GET /channels/{id}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public ResponseEntity<ChannelResponse> getChannel(@PathVariable UUID id) {
		ChannelResponse channel = channelService.find(id);
		return ResponseEntity.ok(channel);
	}

	/**
	 * 공개 채널 정보 수정
	 * PUT /channels/{id}
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public ResponseEntity<ChannelResponse> updateChannel(
			@PathVariable UUID id,
			@RequestBody ChannelUpdateRequest request
	) {
		ChannelResponse channel = channelService.update(id, request);
		return ResponseEntity.ok(channel);
	}

	/**
	 * 채널 삭제
	 * DELETE /channels/{id}
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public ResponseEntity<Void> deleteChannel(@PathVariable UUID id) {
		channelService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
