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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
	 * 사용자 등록 (JSON)
	 * POST /users
	 * Content-Type: application/json
	 */
	@RequestMapping(method = RequestMethod.POST, consumes = "application/json")
	public ResponseEntity<UserResponse> createUser(
			@RequestBody UserCreateRequest userRequest
	) {
		// 프로필 이미지는 null로 전달
		UserResponse user = userService.create(userRequest, null);
		return ResponseEntity.status(HttpStatus.CREATED).body(user);
	}

	/**
	 * 사용자 등록 (프로필 이미지 포함)
	 * POST /users/with-profile
	 * Content-Type: multipart/form-data
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/with-profile", consumes = "multipart/form-data")
	public ResponseEntity<UserResponse> createUserWithProfile(
			@RequestParam("username") String username,
			@RequestParam("email") String email,
			@RequestParam("password") String password,
			@RequestParam(value = "profileImage", required = false) MultipartFile profileImage
	) {
		try {
			// 사용자 생성 요청 객체 생성
			UserCreateRequest userRequest = new UserCreateRequest(username, email, password);

			// 프로필 이미지가 있으면 BinaryContentCreateRequest로 변환
			BinaryContentCreateRequest profileRequest = null;
			if (profileImage != null && !profileImage.isEmpty()) {
				profileRequest = new BinaryContentCreateRequest(
						profileImage.getOriginalFilename(),
						profileImage.getContentType(),
						profileImage.getBytes()
				);
			}

			// 사용자 생성
			UserResponse user = userService.create(userRequest, profileRequest);
			return ResponseEntity.status(HttpStatus.CREATED).body(user);

		} catch (Exception e) {
			throw new RuntimeException("프로필 이미지 업로드 실패: " + e.getMessage(), e);
		}
	}

	/**
	 * 모든 사용자 조회
	 * GET /users
	 * 사용자 목록을 UserDto 형식으로 반환합니다.
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<UserDto>> getAllUsers() {
		List<UserDto> users = userService.findAllAsDto();
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
	 * 사용자 정보 수정 (JSON)
	 * PUT /users/{id}
	 * Content-Type: application/json
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}", consumes = "application/json")
	public ResponseEntity<UserResponse> updateUser(
			@PathVariable UUID id,
			@RequestBody UserUpdateRequest userRequest
	) {
		// 프로필 이미지는 null로 전달
		UserResponse user = userService.update(id, userRequest, null);
		return ResponseEntity.ok(user);
	}

	/**
	 * 사용자 정보 수정 (프로필 이미지 포함)
	 * PUT /users/{id}/with-profile
	 * Content-Type: multipart/form-data
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/with-profile", consumes = "multipart/form-data")
	public ResponseEntity<UserResponse> updateUserWithProfile(
			@PathVariable UUID id,
			@RequestParam("username") String username,
			@RequestParam("email") String email,
			@RequestParam("password") String password,
			@RequestParam(value = "profileImage", required = false) MultipartFile profileImage
	) {
		try {
			// 사용자 수정 요청 객체 생성
			UserUpdateRequest userRequest = new UserUpdateRequest(username, email, password);

			// 프로필 이미지가 있으면 BinaryContentCreateRequest로 변환
			BinaryContentCreateRequest profileRequest = null;
			if (profileImage != null && !profileImage.isEmpty()) {
				profileRequest = new BinaryContentCreateRequest(
						profileImage.getOriginalFilename(),
						profileImage.getContentType(),
						profileImage.getBytes()
				);
			}

			// 사용자 정보 수정
			UserResponse user = userService.update(id, userRequest, profileRequest);
			return ResponseEntity.ok(user);

		} catch (Exception e) {
			throw new RuntimeException("프로필 이미지 업로드 실패: " + e.getMessage(), e);
		}
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
