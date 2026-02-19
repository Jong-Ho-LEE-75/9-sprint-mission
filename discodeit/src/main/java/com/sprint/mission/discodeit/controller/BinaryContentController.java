package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.service.BinaryContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/binaryContents")
@RequiredArgsConstructor
public class BinaryContentController {

	private final BinaryContentService binaryContentService;

	/**
	 * GET /api/binaryContents/{binaryContentId}
	 */
	@GetMapping("/{binaryContentId}")
	public ResponseEntity<BinaryContent> find(@PathVariable UUID binaryContentId) {
		BinaryContent binaryContent = binaryContentService.find(binaryContentId);
		return ResponseEntity.ok(binaryContent);
	}

	/**
	 * GET /api/binaryContents?binaryContentIds={id1},{id2},...
	 */
	@GetMapping
	public ResponseEntity<List<BinaryContent>> findAllByIdIn(@RequestParam List<UUID> binaryContentIds) {
		List<BinaryContent> binaryContents = binaryContentService.findAllByIdIn(binaryContentIds);
		return ResponseEntity.ok(binaryContents);
	}
}
