package com.example.onboarding.controller;

import com.example.onboarding.entity.ChatRoom;
import com.example.onboarding.repository.ChatRoomRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채팅방 REST API 컨트롤러
 * - 채팅방 생성, 조회 등의 기능 제공
 */
@Tag(name = "Chat Room", description = "채팅방 생성 및 조회 API")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    @Operation(summary = "모든 채팅방 조회", description = "생성된 모든 채팅방의 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공")
    @GetMapping
    public ResponseEntity<List<ChatRoom>> getAllRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        return ResponseEntity.ok(rooms);
    }

    @Operation(summary = "특정 채팅방 조회", description = "ID를 사용하여 특정 채팅방의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅방 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 채팅방을 찾을 수 없음")
    })
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoom> getRoom(@PathVariable Long roomId) {
        return chatRoomRepository.findById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "새 채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @ApiResponse(responseCode = "200", description = "채팅방 생성 성공")
    @PostMapping
    public ResponseEntity<ChatRoom> createRoom(@RequestBody CreateRoomRequest request) {
        ChatRoom room = ChatRoom.builder()
                .name(request.name())
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(room);
        return ResponseEntity.ok(savedRoom);
    }

    /**
     * 채팅방 생성 요청 DTO
     */
    public record CreateRoomRequest(String name) {
    }
}
