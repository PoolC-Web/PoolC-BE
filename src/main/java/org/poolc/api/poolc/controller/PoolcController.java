package org.poolc.api.poolc.controller;

import lombok.RequiredArgsConstructor;
import org.poolc.api.poolc.dto.CreatePoolcRequest;
import org.poolc.api.poolc.dto.PoolcResponse;
import org.poolc.api.poolc.dto.UpdatePoolcRequest;
import org.poolc.api.poolc.service.PoolcService;
import org.poolc.api.poolc.vo.PoolcCreateValues;
import org.poolc.api.poolc.vo.PoolcUpdateValues;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/poolc")
@RequiredArgsConstructor
public class PoolcController {
    private final PoolcService poolcService;

    //TODO: poolc 정보를 DB에 직접 넣을 수 있다면 삭제
    @PostMapping
    public ResponseEntity<Void> createPoolc(@RequestBody CreatePoolcRequest request) {
        validateMinimumActivityHours(request.getMinimumActivityHours());
        PoolcCreateValues createValues = new PoolcCreateValues(request);
        poolcService.createPoolc(createValues);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<PoolcResponse> findPoolc() {
        PoolcResponse response = PoolcResponse.of(poolcService.get());
        return ResponseEntity.ok().body(response);
    }

    @PutMapping
    public ResponseEntity<Void> updatePoolc(@RequestBody UpdatePoolcRequest request) {
        validateMinimumActivityHours(request.getMinimumActivityHours());
        PoolcUpdateValues updateValues = new PoolcUpdateValues(request);
        poolcService.updatePoolc(updateValues);
        return ResponseEntity.ok().build();
    }

    private void validateMinimumActivityHours(Integer minimumActivityHours) {
        if (minimumActivityHours != null && minimumActivityHours < 1) {
            throw new IllegalArgumentException("최소 활동 기준은 1시간 이상이어야 합니다.");
        }
    }
}
