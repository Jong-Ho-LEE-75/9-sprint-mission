package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.service.BinaryContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 바이너리 콘텐츠 관련 REST API 컨트롤러
 *
 * 파일(프로필 이미지, 첨부파일 등) 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/binary-contents")
@RequiredArgsConstructor
public class BinaryContentController {

	private final BinaryContentService binaryContentService;

	/**
	 * 바이너리 파일 1개 조회
	 * GET /binary-contents/{id}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public ResponseEntity<BinaryContent> getBinaryContent(@PathVariable UUID id) {
		BinaryContent binaryContent = binaryContentService.find(id);
		return ResponseEntity.ok(binaryContent);
	}

	/**
	 * 여러 바이너리 파일 조회
	 * GET /binary-contents?ids={id1},{id2},...
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<BinaryContent>> getBinaryContents(@RequestParam List<UUID> ids) {
		List<BinaryContent> binaryContents = binaryContentService.findAllByIdIn(ids);
		return ResponseEntity.ok(binaryContents);
	}

}
