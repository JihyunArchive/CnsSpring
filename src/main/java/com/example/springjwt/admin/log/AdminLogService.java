package com.example.springjwt.admin.log;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;
    /**
     * 로그저장
     *
     * @param adminUsername 관리자 ID
     * @param action 수행한 작업 이름 (예: DELETE_POST, BLOCK_USER 등)
     * @param targetType 작업 대상 타입 (예: TRADE_POST, USER 등)
     * @param targetId 작업 대상 ID
     * @param reason 작업 사유
     */
    public void logAdminAction(String adminUsername, String action, String targetType, Long targetId, String reason) {
        AdminLog log = AdminLog.builder()
                .adminUsername(adminUsername)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .build();
        adminLogRepository.save(log);
    }
}
/**
 * 로그 기록 서비스
 *
 * 관리자 기능에서 어떤 작업이 수행되었는지 로그로 남기기 위한 서비스
 *
 * 사용 예시:
 * adminLogService.logAdminAction(
 *     "admin01",          // 관리자 ID
 *     "DELETE_POST",      // 행동 (예: DELETE_POST, BLOCK_USER 등)
 *     "TRADE_POST",       // 대상 타입 (예: TRADE_POST, RECIPE 등)
 *     123L,               // 대상 ID
 *     "허위 정보 포함으로 삭제함"  // 사유
 * );
 *
 */
