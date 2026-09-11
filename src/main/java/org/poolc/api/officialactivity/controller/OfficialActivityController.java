package org.poolc.api.officialactivity.controller;

import lombok.RequiredArgsConstructor;
import org.poolc.api.officialactivity.dto.CreateOfficialActivityRequest;
import org.poolc.api.officialactivity.dto.OfficialActivityCheckInResponse;
import org.poolc.api.officialactivity.dto.OfficialActivityResponse;
import org.poolc.api.officialactivity.dto.OfficialActivityQrResponse;
import org.poolc.api.member.domain.Member;
import org.poolc.api.officialactivity.service.OfficialActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/official-activities")
public class OfficialActivityController {
    private final OfficialActivityService officialActivityService;

    @GetMapping
    public ResponseEntity<List<OfficialActivityResponse>> findAll() {
        return ResponseEntity.ok(officialActivityService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfficialActivityResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(officialActivityService.findById(id));
    }

    @PostMapping
    public ResponseEntity<OfficialActivityResponse> create(@RequestBody @Valid CreateOfficialActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(officialActivityService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfficialActivityResponse> update(@PathVariable Long id, @RequestBody @Valid CreateOfficialActivityRequest request) {
        return ResponseEntity.ok(officialActivityService.update(id, request));
    }

    @PostMapping("/{id}/qr")
    public ResponseEntity<OfficialActivityQrResponse> generateQr(@PathVariable Long id) {
        return ResponseEntity.ok(officialActivityService.generateQr(id));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<OfficialActivityQrResponse> getQr(@PathVariable Long id) {
        return ResponseEntity.ok(officialActivityService.getQr(id));
    }

    @DeleteMapping("/{id}/qr")
    public ResponseEntity<OfficialActivityResponse> disableQr(@PathVariable Long id) {
        return ResponseEntity.ok(officialActivityService.disableQr(id));
    }

    @PostMapping("/check-in/{token}")
    public ResponseEntity<OfficialActivityCheckInResponse> checkIn(@PathVariable String token, @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(officialActivityService.checkIn(token, member.getLoginID()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        officialActivityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
