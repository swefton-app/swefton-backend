package com.swefton.backend.modules.post.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.swefton.backend.modules.post.dto.*;
import com.swefton.backend.modules.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1")
public class PostController {
    private final PostService service;
    @GetMapping("/posts") public PostPageResponse feed(@RequestParam(required=false) Long facilityId, @RequestParam(required=false) String authorType,
            @RequestParam(defaultValue="false") boolean mine,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required=false) Long cursorId, @RequestParam(defaultValue="12") int size) { return service.feed(facilityId, authorType, mine, cursorCreatedAt, cursorId, size); }
    @PostMapping("/posts") public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @GetMapping("/posts/{id}") public PostResponse get(@PathVariable Long id) { return service.get(id); }
    @PutMapping("/posts/{id}") public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest request) { return service.update(id, request); }
    @DeleteMapping("/posts/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
    @PutMapping("/posts/{id}/like") public ResponseEntity<Void> like(@PathVariable Long id) { service.like(id); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/posts/{id}/like") public ResponseEntity<Void> unlike(@PathVariable Long id) { service.unlike(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/posts/{id}/comments") public List<PostCommentResponse> comments(@PathVariable Long id) { return service.listComments(id); }
    @PostMapping("/posts/{id}/comments") public ResponseEntity<PostCommentResponse> comment(@PathVariable Long id, @Valid @RequestBody PostCommentRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.comment(id, request)); }
    @PutMapping("/comments/{id}") public PostCommentResponse updateComment(@PathVariable Long id, @Valid @RequestBody PostCommentRequest request) { return service.updateComment(id, request); }
    @DeleteMapping("/comments/{id}") public ResponseEntity<Void> deleteComment(@PathVariable Long id) { service.deleteComment(id); return ResponseEntity.noContent().build(); }
}
