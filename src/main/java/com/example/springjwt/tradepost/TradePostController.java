package com.example.springjwt.tradepost;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.User.UserService;
import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.tradepost.request.TradeCompleteRequest;
import com.example.springjwt.tradepost.request.TradeCompleteRequestRepository;
import com.example.springjwt.tradepost.request.TradeCompleteRequestService;
import com.example.springjwt.tradepost.request.UserSimpleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.springjwt.tradepost.up.TradePostUpService;
import com.example.springjwt.tradepost.up.TradePostUpResult;
import java.util.List;

@RestController
@RequestMapping("/api/trade-posts")
@RequiredArgsConstructor
public class TradePostController {

    private final TradePostService tradePostService;
    private final UserService userService;
    private final TradeCompleteRequestService tradeCompleteRequestService;
    private final TradeCompleteRequestRepository tradeCompleteRequestRepository;
    private final UserRepository userRepository;
    private final TradePostUpService tradePostUpService;

    // 거래글 생성
    @PostMapping
    public ResponseEntity<TradePostDTO> createTradePost(@RequestBody TradePostDTO dto,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        TradePost post = tradePostService.create(dto, userDetails.getUsername());
        return ResponseEntity.ok(TradePostDTO.fromEntity(post));
    }

    // 거래글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<TradePostDTO> getTradePostById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        TradePostDTO dto = tradePostService.getTradePostById(id, username);
        return ResponseEntity.ok(dto);
    }

    // 내가 작성한 거래글
    @GetMapping("/my-posts")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getMyTradePosts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<TradePostSimpleResponseDTO> myPosts = tradePostService.getMyTradePosts(userDetails.getUsername());
        return ResponseEntity.ok(myPosts);
    }

    // 전체 거래글
    @GetMapping
    public ResponseEntity<List<TradePostDTO>> getAllTradePosts() {
        List<TradePostDTO> tradePosts = tradePostService.getAllTradePosts();
        return ResponseEntity.ok(tradePosts);
    }

    // 카테고리 필터
    @GetMapping("/category")
    public ResponseEntity<List<TradePostDTO>> getTradePostsByCategory(@RequestParam("category") String category) {
        List<TradePostDTO> tradePosts = tradePostService.getTradePostsByCategory(category);
        return ResponseEntity.ok(tradePosts);
    }

    // 키워드 검색
    @GetMapping("/search")
    public ResponseEntity<List<TradePostDTO>> searchTradePosts(@RequestParam("keyword") String keyword) {
        List<TradePostDTO> result = tradePostService.searchTradePosts(keyword);
        return ResponseEntity.ok(result);
    }

    // 사용자 위치 기준 1km 이내 거래글
    @GetMapping("/nearby")
    public ResponseEntity<List<TradePostDTO>> getNearbyTradePosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1.0") double distanceKm
    ) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        List<TradePostDTO> nearby = tradePostService.getNearbyTradePosts(username, distanceKm);
        return ResponseEntity.ok(nearby);
    }

    // 거리순
    @GetMapping("/sorted-by-distance")
    public ResponseEntity<List<TradePostDTO>> getSortedByDistance(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        List<TradePostDTO> posts = tradePostService.getTradePostsSortedByDistance(username);
        return ResponseEntity.ok(posts);
    }

    // 카테고리 + 거리순 필터링
    @GetMapping("/nearby/filter")
    public ResponseEntity<List<TradePostDTO>> getNearbyByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam double distanceKm,
            @RequestParam String category
    ) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        List<TradePostDTO> result = tradePostService.getNearbyByCategory(username, distanceKm, category);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/nearby-by-multiple-categories")
    public ResponseEntity<List<TradePostDTO>> getNearbyPostsByMultipleCategories(
            @RequestParam double distanceKm,
            @RequestParam List<String> categories,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<TradePostDTO> posts = tradePostService.getNearbyPostsByMultipleCategories(
                userDetails.getUserEntity(), distanceKm, categories
        );
        return ResponseEntity.ok(posts);
    }

    // 조회수 기준 인기 거래글 3개
    @GetMapping("/popular")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getPopularTradePosts() {
        List<TradePostSimpleResponseDTO> topPosts = tradePostService.getTop3PopularTradePosts();
        return ResponseEntity.ok(topPosts);
    }

    @PatchMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        tradePostService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/complete-request")
    public ResponseEntity<String> requestComplete(@PathVariable Long postId,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        return tradeCompleteRequestService.createRequest(postId, userDetails.getUsername());
    }

    @GetMapping("/{id}/complete-requests")
    public ResponseEntity<List<UserSimpleDTO>> getRequesters(@PathVariable Long id) {
        List<TradeCompleteRequest> list = tradeCompleteRequestRepository.findByTradePost_TradePostId(id);
        return ResponseEntity.ok(
                list.stream().map(req -> UserSimpleDTO.fromEntity(req.getRequester())).toList()
        );
    }

    @GetMapping("/mypurchases")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getMyPurchasedPosts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<TradePostSimpleResponseDTO> purchases = tradePostService.getMyPurchasedPosts(userDetails.getUsername());
        return ResponseEntity.ok(purchases);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TradePostDTO> completeTradePost(
            @PathVariable Long id,
            @RequestParam Long buyerId
    ) {
        TradePost completedPost = tradePostService.completeTradePost(id, buyerId);
        return ResponseEntity.ok(TradePostDTO.fromEntity(completedPost));
    }
    
    @GetMapping("/user/{username}")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getPostsByUsername(@PathVariable String username) {
        return ResponseEntity.ok(tradePostService.getPostsByUsername(username));
    }

    @GetMapping("/user/{username}/status/{status}")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getPostsByUsernameAndStatus(
            @PathVariable String username,
            @PathVariable int status
    ) {
        return ResponseEntity.ok(tradePostService.getPostsByUsernameAndStatus(username, status));
    }
    @GetMapping("/info")
    public ResponseEntity<UserProfileResponseDTO> getUserProfileInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        UserEntity user = userRepository.findByUsername(username);
        return ResponseEntity.ok(tradePostService.getUserProfile(user));
    }

    // 끌어올리기
    @PostMapping("/{id}/up")
    public ResponseEntity<TradePostUpResult> up(
            @PathVariable Long id,
            @AuthenticationPrincipal com.example.springjwt.dto.CustomUserDetails userDetails
    ) {
        var result = tradePostUpService.up(id, userDetails.getUserEntity());
        return ResponseEntity.ok(result);
    }
}
