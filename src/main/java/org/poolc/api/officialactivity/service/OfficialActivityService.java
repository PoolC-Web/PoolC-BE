package org.poolc.api.officialactivity.service;

import lombok.RequiredArgsConstructor;
import org.poolc.api.common.exception.ConflictException;
import org.poolc.api.member.repository.MemberRepository;
import org.poolc.api.member.domain.Member;
import org.poolc.api.officialactivity.domain.OfficialActivity;
import org.poolc.api.officialactivity.domain.OfficialActivityParticipantSource;
import org.poolc.api.officialactivity.domain.OfficialActivityQrAttendance;
import org.poolc.api.officialactivity.domain.OfficialActivityQrToken;
import org.poolc.api.officialactivity.dto.CreateOfficialActivityRequest;
import org.poolc.api.officialactivity.dto.OfficialActivityParticipantResponse;
import org.poolc.api.officialactivity.dto.OfficialActivityQrResponse;
import org.poolc.api.officialactivity.dto.OfficialActivityResponse;
import org.poolc.api.officialactivity.repository.OfficialActivityRepository;
import org.poolc.api.officialactivity.repository.OfficialActivityQrAttendanceRepository;
import org.poolc.api.officialactivity.repository.OfficialActivityQrTokenRepository;
import org.poolc.api.tool.domain.Qr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfficialActivityService {
    private final OfficialActivityRepository officialActivityRepository;
    private final MemberRepository memberRepository;
    private final OfficialActivityQrAttendanceRepository qrAttendanceRepository;
    private final OfficialActivityQrTokenRepository qrTokenRepository;

    @Value("${official-activity.qr.base-url:https://poolc.org/official-activities/check-in}")
    private String checkInBaseUrl;

    @Transactional
    public OfficialActivityResponse create(CreateOfficialActivityRequest request) {
        List<String> memberLoginIds = request.getMemberLoginIds().stream().distinct().collect(Collectors.toList());
        Map<String, Member> membersByLoginId = membersByLoginId(memberLoginIds);
        if (membersByLoginId.size() != memberLoginIds.size()) {
            throw new NoSuchElementException("회원을 찾을 수 없습니다.");
        }
        OfficialActivity activity = officialActivityRepository.save(new OfficialActivity(
                memberLoginIds, request.getActivityDate(), request.getTitle().trim(), request.getRecognizedHours()));
        return responseOf(activity, membersByLoginId);
    }

    @Transactional(readOnly = true)
    public List<OfficialActivityResponse> findAll() {
        List<OfficialActivity> activities = officialActivityRepository.findAll();
        List<String> memberLoginIds = activities.stream()
                .flatMap(activity -> activity.getMemberLoginIds().stream())
                .distinct()
                .collect(Collectors.toList());
        Map<String, Member> membersByLoginId = membersByLoginId(memberLoginIds);
        return activities.stream()
                .sorted(Comparator.comparing(OfficialActivity::getCreatedAt).reversed())
                .map(activity -> responseOf(activity, membersByLoginId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OfficialActivityResponse findById(Long id) {
        OfficialActivity activity = findActivity(id);
        return responseOf(activity, membersByLoginId(activity.getMemberLoginIds()));
    }

    @Transactional
    public OfficialActivityResponse update(Long id, CreateOfficialActivityRequest request) {
        OfficialActivity activity = findActivity(id);
        List<String> memberLoginIds = request.getMemberLoginIds().stream().distinct().collect(Collectors.toList());
        Map<String, Member> membersByLoginId = membersByLoginId(memberLoginIds);
        if (membersByLoginId.size() != memberLoginIds.size()) {
            throw new NoSuchElementException("회원을 찾을 수 없습니다.");
        }
        activity.update(memberLoginIds, request.getActivityDate(), request.getTitle().trim(), request.getRecognizedHours());
        return responseOf(activity, membersByLoginId);
    }

    @Transactional
    public void delete(Long id) {
        officialActivityRepository.delete(findActivity(id));
    }

    @Transactional
    public OfficialActivityQrResponse generateQr(Long id) {
        OfficialActivity activity = findActivity(id);
        qrTokenRepository.findByOfficialActivityId(id).ifPresent(qrTokenRepository::delete);
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);
        qrTokenRepository.save(new OfficialActivityQrToken(token, activity, expiresAt));
        String checkInUrl = checkInBaseUrl + "/" + token;
        String imageDataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(new Qr(checkInUrl).createQrImage());
        return new OfficialActivityQrResponse(checkInUrl, imageDataUrl, expiresAt);
    }

    @Transactional
    public OfficialActivityResponse checkIn(String token, String memberLoginId) {
        OfficialActivityQrToken qrToken = qrTokenRepository.findById(token)
                .orElseThrow(() -> new ConflictException("유효하지 않은 출석 QR입니다."));
        if (qrToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("출석 QR이 만료되었습니다.");
        }
        OfficialActivity activity = qrToken.getOfficialActivity();
        activity.addMemberLoginId(memberLoginId);
        qrAttendanceRepository.findByOfficialActivityIdAndMemberLoginId(activity.getId(), memberLoginId)
                .orElseGet(() -> qrAttendanceRepository.save(new OfficialActivityQrAttendance(activity, memberLoginId)));
        return responseOf(activity, membersByLoginId(activity.getMemberLoginIds()));
    }

    private OfficialActivity findActivity(Long id) {
        return officialActivityRepository.findById(id)
                .orElseThrow(() -> new ConflictException("공식 활동을 찾을 수 없습니다."));
    }

    private Map<String, Member> membersByLoginId(List<String> memberLoginIds) {
        return memberRepository.findAllMembersByLoginIDList(memberLoginIds).stream()
                .collect(Collectors.toMap(Member::getLoginID, member -> member));
    }

    private OfficialActivityResponse responseOf(OfficialActivity activity, Map<String, Member> membersByLoginId) {
        List<String> memberNames = activity.getMemberLoginIds().stream()
                .map(membersByLoginId::get)
                .filter(member -> member != null)
                .map(Member::getName)
                .collect(Collectors.toList());
        Set<String> qrAttendanceLoginIds = qrAttendanceRepository.findByOfficialActivityIdIn(List.of(activity.getId())).stream()
                .map(OfficialActivityQrAttendance::getMemberLoginId)
                .collect(Collectors.toSet());
        List<OfficialActivityParticipantResponse> participants = activity.getMemberLoginIds().stream()
                .map(loginId -> {
                    Member member = membersByLoginId.get(loginId);
                    return new OfficialActivityParticipantResponse(loginId, member == null ? null : member.getName(),
                            member == null ? null : member.getDepartment(), member == null ? null : member.getStudentID(),
                            member == null ? null : member.getPhoneNumber(), qrAttendanceLoginIds.contains(loginId) ? OfficialActivityParticipantSource.QR : OfficialActivityParticipantSource.MANUAL);
                })
                .collect(Collectors.toList());
        return new OfficialActivityResponse(activity, memberNames, participants);
    }
}
