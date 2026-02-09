package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 사용자 관련 REST API 컨트롤러
 *
 * 사용자 생성, 조회, 수정, 삭제 및 온라인 상태 관리 기능을 제공합니다.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserStatusService userStatusService;

	/**
	 * 사용자 등록
	 * POST /users
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<UserResponse> createUser(
			@RequestBody UserCreateRequest userRequest
	) {
		// 프로필 이미지는 null로 전달 (별도 엔드포인트로 업로드 가능)
		UserResponse user = userService.create(userRequest, null);
		return ResponseEntity.status(HttpStatus.CREATED).body(user);
	}

	/**
	 * 모든 사용자 조회
	 * GET /users
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		List<UserResponse> users = userService.findAll();
		return ResponseEntity.ok(users);
	}

	/**
	 * 특정 사용자 조회
	 * GET /users/{id}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
		UserResponse user = userService.find(id);
		return ResponseEntity.ok(user);
	}

	/**
	 * 사용자 정보 수정
	 * PUT /users/{id}
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public ResponseEntity<UserResponse> updateUser(
			@PathVariable UUID id,
			@RequestBody UserUpdateRequest userRequest
	) {
		// 프로필 이미지는 null로 전달 (별도 엔드포인트로 업로드 가능)
		UserResponse user = userService.update(id, userRequest, null);
		return ResponseEntity.ok(user);
	}

	/**
	 * 사용자 삭제
	 * DELETE /users/{id}
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 온라인 상태 업데이트
	 * PUT /users/{id}/status
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/status")
	public ResponseEntity<UserStatus> updateUserStatus(
			@PathVariable UUID id,
			@RequestBody UserStatusUpdateRequest request
	) {
		UserStatus userStatus = userStatusService.updateByUserId(id, request);
		return ResponseEntity.ok(userStatus);
	}
}
