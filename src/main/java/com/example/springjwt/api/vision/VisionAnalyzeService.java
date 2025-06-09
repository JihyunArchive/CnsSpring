package com.example.springjwt.api.vision;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.api.GoogleTranslateService;
import com.example.springjwt.fridge.Fridge;
import com.example.springjwt.fridge.FridgeRepository;
import com.example.springjwt.fridge.FridgeService;
import com.example.springjwt.fridge.UnitCategory;
import com.example.springjwt.recipe.cashe.IngredientNameCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisionAnalyzeService {

    private final GcpVisionClient gcpVisionClient;
    private final FridgeRepository fridgeRepository;
    private final FridgeService fridgeService;
    private final IngredientNameCache ingredientNameCache;
    private final IngredientParser ingredientParser;
    private final GoogleTranslateService googleTranslateService;

    public List<String> analyzeAndSave(MultipartFile imageFile, UserEntity user) {
        List<String> detectedLabels = gcpVisionClient.detectLabels(imageFile); // 예: ["onion", "table", "apple"]
        System.out.println("📸 [VisionAnalyzeService] Vision 결과 라벨: " + detectedLabels);

        List<String> savedIngredients = new ArrayList<>();

        // 전체 캐시된 재료명 가져오기
        Set<String> allKorNames = ingredientNameCache.getAll(); // 예: [양파, 당근, 우유]

        // 한글 → 영문 번역 (역매핑 준비)
        Map<String, String> korToEng = googleTranslateService.translateBatch(new ArrayList<>(allKorNames));
        System.out.println("🧠 [VisionAnalyzeService] 번역 매핑 (한→영): " + korToEng);
        // 영문 → 한글 역매핑 생성
        Map<String, String> engToKor = korToEng.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getValue().toLowerCase(), Map.Entry::getKey));
        System.out.println("🧠 [VisionAnalyzeService] 역매핑 (영→한): " + engToKor);
        Long userId = (long) user.getId();

        for (String label : detectedLabels) {
            String labelLower = label.toLowerCase();

            if (engToKor.containsKey(labelLower)) {
                String korName = engToKor.get(labelLower); // 예: "onion" → "양파"

                Fridge fridge = new Fridge();
                fridge.setIngredientName(korName);
                fridge.setStorageArea("냉장");
                fridge.setQuantity(1.0);
                fridge.setUnitCategory(UnitCategory.COUNT);
                fridge.setUnitDetail("개");
                fridge.setFridgeDate(LocalDate.now());
                fridge.setCreatedAt(LocalDateTime.now());
                fridge.setUpdatedAt(LocalDateTime.now());
                fridge.setUser(user);

                fridgeService.createFridge(fridge, userId); // 히스토리도 같이 저장됨
                System.out.println("✅ [VisionAnalyzeService] 저장 시도: " + korName);

                savedIngredients.add(korName);
                System.out.println("📦 [VisionAnalyzeService] 재료 저장 완료: " + korName);

            }
        }

        return savedIngredients;
    }
}
