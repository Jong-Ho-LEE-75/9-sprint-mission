package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.LoginRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 REST API 컨트롤러
 *
 * 사용자 로그인 기능을 제공합니다.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/**
	 * 로그인
	 * POST /auth/login
	 */
	@RequestMapping(
			method = RequestMethod.POST,
			path = "login",
			consumes = {MediaType.APPLICATION_JSON_VALUE}
	)
	public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
		UserResponse user = authService.login(request);
		return ResponseEntity.ok(user);
	}
}
