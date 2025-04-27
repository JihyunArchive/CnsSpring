package com.example.springjwt.market;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade-posts")
@RequiredArgsConstructor
public class TradePostController {
    private final TradePostService tradePostService;

    // 거래글 생성
    @PostMapping
    public ResponseEntity<TradePostDTO> createTradePost(@RequestBody TradePostDTO dto,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        TradePost post = tradePostService.create(dto, userDetails.getUsername());
        return ResponseEntity.ok(TradePostDTO.fromEntity(post));
    }

    // 거래글 상세 조회 (ID로)
    @GetMapping("/{id}")
    public ResponseEntity<TradePostDTO> getTradePostById(@PathVariable Long id,@AuthenticationPrincipal UserDetails userDetails) {
        TradePostDTO tradePostDTO = tradePostService.getTradePostById(id);
        return ResponseEntity.ok(tradePostDTO);
    }

    // 거래 완료 처리
    @PatchMapping("/{id}/complete")
    public ResponseEntity<TradePostDTO> completeTradePost(@PathVariable Long id) {
        TradePost completedPost = tradePostService.completeTradePost(id);
        return ResponseEntity.ok(TradePostDTO.fromEntity(completedPost));
    }

    // 내가 작성한 거래글 조회
    @GetMapping("/my-posts")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getMyTradePosts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<TradePostSimpleResponseDTO> myPosts = tradePostService.getMyTradePosts(userDetails.getUsername());
        return ResponseEntity.ok(myPosts);
    }

    // 거래글 불러오기(메인에서 조회)
    @GetMapping
    public ResponseEntity<List<TradePostDTO>> getAllTradePosts() {
        List<TradePostDTO> tradePosts = tradePostService.getAllTradePosts();
        return ResponseEntity.ok(tradePosts);
    }

    //거래글 카테고리 필터링
    @GetMapping("/category")
    public ResponseEntity<List<TradePostDTO>> getTradePostsByCategory(@RequestParam("category") String category) {
        List<TradePostDTO> tradePosts = tradePostService.getTradePostsByCategory(category);
        return ResponseEntity.ok(tradePosts);
    }

    // 거래글 검색
    @GetMapping("/search")
    public ResponseEntity<List<TradePostDTO>> searchTradePosts(
            @RequestParam("keyword") String keyword) {
        List<TradePostDTO> result = tradePostService.searchTradePosts(keyword);
        return ResponseEntity.ok(result);
    }

}
