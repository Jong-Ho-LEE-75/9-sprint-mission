package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserStatusService userStatusService;

	/**
	 * POST /api/users
	 * multipart/form-data: userCreateRequest(JSON part) + profile(binary, optional)
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UserResponse> create(
			@RequestPart("userCreateRequest") UserCreateRequest userCreateRequest,
			@RequestPart(value = "profile", required = false) MultipartFile profile
	) throws IOException {
		BinaryContentCreateRequest profileRequest = resolveProfile(profile);
		UserResponse user = userService.create(userCreateRequest, profileRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(user);
	}

	/**
	 * GET /api/users
	 */
	@GetMapping
	public ResponseEntity<List<UserDto>> findAll() {
		List<UserDto> users = userService.findAllAsDto();
		return ResponseEntity.ok(users);
	}

	/**
	 * PATCH /api/users/{userId}
	 * multipart/form-data: userUpdateRequest(JSON part) + profile(binary, optional)
	 */
	@PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UserResponse> update(
			@PathVariable UUID userId,
			@RequestPart("userUpdateRequest") UserUpdateRequest userUpdateRequest,
			@RequestPart(value = "profile", required = false) MultipartFile profile
	) throws IOException {
		BinaryContentCreateRequest profileRequest = resolveProfile(profile);
		UserResponse user = userService.update(userId, userUpdateRequest, profileRequest);
		return ResponseEntity.ok(user);
	}

	/**
	 * DELETE /api/users/{userId}
	 */
	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> delete(@PathVariable UUID userId) {
		userService.delete(userId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * PATCH /api/users/{userId}/userStatus
	 */
	@PatchMapping(value = "/{userId}/userStatus", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UserStatus> updateUserStatus(
			@PathVariable UUID userId,
			@RequestBody UserStatusUpdateRequest request
	) {
		UserStatus userStatus = userStatusService.updateByUserId(userId, request);
		return ResponseEntity.ok(userStatus);
	}

	private BinaryContentCreateRequest resolveProfile(MultipartFile profile) throws IOException {
		if (profile == null || profile.isEmpty()) {
			return null;
		}
		return new BinaryContentCreateRequest(
				profile.getOriginalFilename(),
				profile.getContentType(),
				profile.getBytes()
		);
	}
}
