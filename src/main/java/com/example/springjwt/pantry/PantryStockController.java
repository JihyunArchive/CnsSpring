package com.example.springjwt.pantry;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.ingredient.IngredientMaster;
import com.example.springjwt.ingredient.IngredientMasterRepository;
import com.example.springjwt.ingredient.UnitEntity;
import com.example.springjwt.ingredient.UnitRepository;
import com.example.springjwt.pantry.dto.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/pantries/{pantryId}/stocks")
@RequiredArgsConstructor
public class PantryStockController {

    private final PantryRepository pantryRepository;
    private final IngredientMasterRepository ingredientRepo;
    private final UnitRepository unitRepo;
    private final PantryStockRepository pantryStockRepository;
    private final PantryHistoryRepository historyRepository;

    // 재고 추가
    @PostMapping
    @Transactional
    public ResponseEntity<PantryStockResponse> add(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId,
            @RequestBody PantryStockCreateRequest req
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();

        Pantry pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고를 찾을 수 없습니다. id=" + pantryId));

        IngredientMaster ing = ingredientRepo.findById(req.getIngredientId())
                .orElseThrow(() -> new IllegalArgumentException("재료를 찾을 수 없습니다. id=" + req.getIngredientId()));

        UnitEntity unit = unitRepo.findById(req.getUnitId())
                .orElseThrow(() -> new IllegalArgumentException("단위를 찾을 수 없습니다. id=" + req.getUnitId()));

        BigDecimal qty = parseQuantity(req.getQuantity());
        StorageLocation storage = parseStorage(req.getStorage());
        StockSource source = parseSource(req.getSource());
        LocalDate purchasedAt = parseDateOrNull(req.getPurchasedAt());
        LocalDate expiresAt   = parseDateOrNull(req.getExpiresAt());

        PantryStock saved = pantryStockRepository.save(
                PantryStock.builder()
                        .pantry(pantry)
                        .ingredient(ing)
                        .unit(unit)
                        .quantity(qty)
                        .storage(storage)
                        .purchasedAt(purchasedAt)
                        .expiresAt(expiresAt)
                        .memo(req.getMemo())
                        .source(source)
                        .build()
        );

        historyRepository.save(
                PantryHistory.builder()
                        .pantry(pantry)
                        .ingredient(ing)
                        .unit(unit)
                        .changeQty(qty)
                        .action(HistoryAction.ADD)
                        .stock(saved)
                        .refType("PANTRY_STOCK")
                        .refId(saved.getId())
                        .note(req.getMemo())
                        .build()
        );

        PantryStockResponse body = PantryStockResponse.builder().id(saved.getId()).build();
        URI location = URI.create("/api/pantries/" + pantryId + "/stocks/" + saved.getId());
        return ResponseEntity.created(location).body(body);
    }

    // 재고 목록 조회 (유통기한 오름차순 → 생성일 내림차순, null expiresAt는 마지막)
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<PantryStockDto>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();
        Pantry pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고를 찾을 수 없습니다. id=" + pantryId));

        List<PantryStock> stocks = pantryStockRepository.findAllByPantry_Id(pantry.getId());

        Comparator<PantryStock> cmp = Comparator
                .comparing(PantryStock::getExpiresAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PantryStock::getCreatedAt, Comparator.reverseOrder());

        List<PantryStockDto> dtos = stocks.stream()
                .sorted(cmp)
                .map(s -> PantryStockDto.builder()
                        .id(s.getId())

                        // ★ 추가된 필드들
                        .ingredientId(s.getIngredient() != null ? s.getIngredient().getId() : null)
                        .unitId(s.getUnit() != null ? s.getUnit().getId() : null)
                        .purchasedAt(s.getPurchasedAt() == null ? null : s.getPurchasedAt().toString())
                        .expiresAt(s.getExpiresAt()   == null ? null : s.getExpiresAt().toString())

                        // 기존 표시용
                        .ingredientName(safe(s.getIngredient() != null ? s.getIngredient().getNameKo() : null))
                        .category(s.getIngredient() != null && s.getIngredient().getCategory() != null
                                ? s.getIngredient().getCategory().name() : null)
                        .storage(s.getStorage() != null ? s.getStorage().name() : null)
                        .quantity(s.getQuantity() != null ? s.getQuantity().toPlainString() : null)
                        .unitName(s.getUnit() != null ? s.getUnit().getName() : null)
                        .iconUrl(s.getIngredient() != null ? s.getIngredient().getIconUrl() : null)
                        .build()
                )
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // --- 로컬 예외 핸들러: 잘못된 입력은 400으로 반환 ---
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // --- 파싱/유틸 ---

    private BigDecimal parseQuantity(String q) {
        String v = (q == null ? "" : q).trim();
        if (v.isEmpty()) v = "1";
        try {
            BigDecimal bd = new BigDecimal(v);
            if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("수량은 0보다 커야 합니다: " + v);
            }
            return bd.setScale(3, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("수량 형식이 잘못되었습니다: " + v);
        }
    }

    private StorageLocation parseStorage(String s) {
        if (s == null || s.isBlank()) return StorageLocation.FRIDGE;
        try {
            return StorageLocation.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("storage 값이 잘못되었습니다: " + s);
        }
    }

    private StockSource parseSource(String s) {
        if (s == null || s.isBlank()) return StockSource.MANUAL;
        try {
            return StockSource.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("source 값이 잘못되었습니다: " + s);
        }
    }

    private LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim()); // yyyy-MM-dd
        } catch (Exception e) {
            throw new IllegalArgumentException("날짜 형식이 잘못되었습니다(yyyy-MM-dd): " + s);
        }
    }

    private String safe(String v) { return v == null ? "" : v; }

    @GetMapping("/{stockId}")
    @Transactional(readOnly = true)
    public ResponseEntity<PantryStockDetailDto> getOne(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId,
            @PathVariable Long stockId
    ) {
        var user = ((CustomUserDetails) userDetails).getUserEntity();
        var pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고를 찾을 수 없습니다. id=" + pantryId));

        var s = pantryStockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다. id=" + stockId));
        if (!s.getPantry().getId().equals(pantry.getId())) {
            throw new IllegalArgumentException("해당 냉장고의 재고가 아닙니다.");
        }

        var dto = PantryStockDetailDto.builder()
                .id(s.getId())
                .ingredientName(s.getIngredient().getNameKo())
                .category(s.getIngredient().getCategory().name())
                .storage(s.getStorage().name())
                .quantity(s.getQuantity().toPlainString())
                .unitName(s.getUnit().getName())
                .iconUrl(s.getIngredient().getIconUrl())
                .purchasedAt(s.getPurchasedAt() == null ? null : s.getPurchasedAt().toString())
                .unitId(s.getUnit().getId())
                .ingredientId(s.getIngredient().getId())
                .expiresAt(s.getExpiresAt() == null ? null : s.getExpiresAt().toString())
                .build();
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{stockId}")
    @Transactional
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId,
            @PathVariable Long stockId,
            @RequestBody PantryStockUpdateRequest req
    ) {
        var user = ((CustomUserDetails) userDetails).getUserEntity();
        var pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고를 찾을 수 없습니다. id=" + pantryId));

        var s = pantryStockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다. id=" + stockId));
        if (!s.getPantry().getId().equals(pantry.getId())) {
            throw new IllegalArgumentException("해당 냉장고의 재고가 아닙니다.");
        }

        // quantity
        if (req.getQuantity() != null) {
            try { s.setQuantity(new java.math.BigDecimal(req.getQuantity().trim())); }
            catch (Exception e) { throw new IllegalArgumentException("수량 형식이 잘못되었습니다: " + req.getQuantity()); }
        }

        // storage
        if (req.getStorage() != null) {
            s.setStorage(StorageLocation.valueOf(req.getStorage().toUpperCase()));
        }

        // ----- 날짜(배타 로직) -----
        // 1) expiresAt가 값으로 오면, 유통기한 설정 + 구매일자 비우기
        if (req.getExpiresAt() != null) {
            if (req.getExpiresAt().isBlank()) {
                s.setExpiresAt(null);
            } else {
                s.setExpiresAt(java.time.LocalDate.parse(req.getExpiresAt()));
                s.setPurchasedAt(null); // ★ 배타성 보장
            }
        }

        // 2) purchasedAt이 값으로 오면, 구매일자 설정 + 유통기한 비우기
        if (req.getPurchasedAt() != null) {
            if (req.getPurchasedAt().isBlank()) {
                s.setPurchasedAt(null);
            } else {
                s.setPurchasedAt(java.time.LocalDate.parse(req.getPurchasedAt()));
                s.setExpiresAt(null); // ★ 배타성 보장
            }
        }

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{stockId}")
    @Transactional
    public ResponseEntity<Void> deleteOne(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId,
            @PathVariable Long stockId
    ) {
        var user = ((CustomUserDetails) userDetails).getUserEntity();
        var pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고를 찾을 수 없습니다. id=" + pantryId));

        var s = pantryStockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다. id=" + stockId));

        if (!s.getPantry().getId().equals(pantry.getId())) {
            throw new IllegalArgumentException("해당 냉장고의 재고가 아닙니다.");
        }

        pantryStockRepository.delete(s); // <- 여기서 끝 (히스토리는 DB가 SET NULL)
        return ResponseEntity.noContent().build();
    }

    @Data
    static class IdsRequest { private List<Long> ids; }

    @PostMapping("/delete")
    @Transactional
    public ResponseEntity<Void> deleteMany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId,
            @RequestBody IdsRequest req
    ) {
        var user = ((CustomUserDetails) userDetails).getUserEntity();
        var pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고를 찾을 수 없습니다. id=" + pantryId));

        if (req.getIds() == null || req.getIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var stocks = pantryStockRepository.findAllById(req.getIds());
        // 보안: 다른 냉장고 소속 재고가 섞여 있지 않은지 확인
        for (var s : stocks) {
            if (!s.getPantry().getId().equals(pantry.getId())) {
                throw new IllegalArgumentException("다른 냉장고의 재고가 포함되어 있습니다. id=" + s.getId());
            }
        }
        pantryStockRepository.deleteAll(stocks);
        return ResponseEntity.noContent().build();
    }


}
