package com.example.springjwt.chat;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.User.UserService;
import com.example.springjwt.market.TradePost;
import com.example.springjwt.market.TradePostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-room")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final TradePostRepository tradePostRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ChatRoomResponseDTO> createOrGetRoom(
            @RequestParam Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername(); // 로그인한 사용자의 username

        UserEntity sender = userRepository.findByUsername(username);
        if (sender == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TradePost post = tradePostRepository.findById(postId).orElseThrow();
        UserEntity receiver = post.getUser(); // 게시글 작성자

        ChatRoom room = chatRoomService.createOrGetRoom(
                post.getTradePostId(),
                (long) sender.getId(),
                (long) receiver.getId()
        );

        return ResponseEntity.ok(ChatRoomResponseDTO.from(room));
    }
}
