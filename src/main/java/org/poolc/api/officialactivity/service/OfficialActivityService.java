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
import org.poolc.api.officialactivity.dto.OfficialActivityCheckInResponse;
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
import java.util.Collections;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
        Map<Long, Map<String, LocalDateTime>> qrAttendanceTimesByActivityId = qrAttendanceTimesByActivityId(
                activities.stream().map(OfficialActivity::getId).collect(Collectors.toList()));
        return activities.stream()
                .sorted(Comparator.comparing(OfficialActivity::getCreatedAt).reversed())
                .map(activity -> responseOf(activity, membersByLoginId,
                        qrAttendanceTimesByActivityId.getOrDefault(activity.getId(), Collections.emptyMap())))
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
        activity.enableQr();
        qrTokenRepository.save(new OfficialActivityQrToken(token, activity));
        return qrResponseOf(token);
    }

    @Transactional(readOnly = true)
    public OfficialActivityQrResponse getQr(Long id) {
        OfficialActivity activity = findActivity(id);
        if (!activity.isQrEnabled()) {
            throw new ConflictException("현재 출석 QR이 꺼져 있습니다.");
        }
        OfficialActivityQrToken qrToken = qrTokenRepository.findByOfficialActivityId(id)
                .orElseThrow(() -> new ConflictException("출석 QR을 찾을 수 없습니다."));
        return qrResponseOf(qrToken.getToken());
    }

    private OfficialActivityQrResponse qrResponseOf(String token) {
        String checkInUrl = checkInBaseUrl + "/" + token;
        String imageDataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(new Qr(checkInUrl).createQrImage());
        return new OfficialActivityQrResponse(checkInUrl, imageDataUrl);
    }

    @Transactional
    public OfficialActivityResponse disableQr(Long id) {
        OfficialActivity activity = findActivity(id);
        activity.disableQr();
        qrTokenRepository.findByOfficialActivityId(id).ifPresent(qrTokenRepository::delete);
        return responseOf(activity, membersByLoginId(activity.getMemberLoginIds()));
    }

    @Transactional
    public OfficialActivityCheckInResponse checkIn(String token, String memberLoginId) {
        OfficialActivityQrTokenRepository.CheckInToken qrToken = qrTokenRepository.findCheckInTokenByToken(token)
                .orElseThrow(() -> new ConflictException("유효하지 않은 출석 QR입니다."));
        if (!qrToken.getQrEnabled()) {
            throw new ConflictException("현재 출석 QR이 꺼져 있습니다.");
        }
        officialActivityRepository.insertMemberIfAbsent(qrToken.getActivityId(), memberLoginId);
        boolean alreadyCheckedIn = qrAttendanceRepository.insertIfAbsent(qrToken.getActivityId(), memberLoginId) == 0;
        return new OfficialActivityCheckInResponse(alreadyCheckedIn);
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
        return responseOf(activity, membersByLoginId, qrAttendanceTimesByActivityId(List.of(activity.getId()))
                .getOrDefault(activity.getId(), Collections.emptyMap()));
    }

    private OfficialActivityResponse responseOf(OfficialActivity activity, Map<String, Member> membersByLoginId,
                                                Map<String, LocalDateTime> qrAttendanceTimesByLoginId) {
        List<String> memberNames = activity.getMemberLoginIds().stream()
                .map(membersByLoginId::get)
                .filter(member -> member != null)
                .map(Member::getName)
                .collect(Collectors.toList());
        List<OfficialActivityParticipantResponse> participants = activity.getMemberLoginIds().stream()
                .map(loginId -> {
                    Member member = membersByLoginId.get(loginId);
                    LocalDateTime qrCheckedInAt = qrAttendanceTimesByLoginId.get(loginId);
                    return new OfficialActivityParticipantResponse(loginId, member == null ? null : member.getName(),
                            member == null ? null : member.getDepartment(), member == null ? null : member.getStudentID(),
                            member == null ? null : member.getPhoneNumber(), qrCheckedInAt != null ? OfficialActivityParticipantSource.QR : OfficialActivityParticipantSource.MANUAL,
                            qrCheckedInAt != null ? qrCheckedInAt : activity.getCreatedAt());
                })
                .collect(Collectors.toList());
        return new OfficialActivityResponse(activity, memberNames, participants);
    }

    private Map<Long, Map<String, LocalDateTime>> qrAttendanceTimesByActivityId(List<Long> activityIds) {
        return qrAttendanceRepository.findByOfficialActivityIdIn(activityIds).stream()
                .collect(Collectors.groupingBy(
                        attendance -> attendance.getOfficialActivity().getId(),
                        Collectors.toMap(OfficialActivityQrAttendance::getMemberLoginId, OfficialActivityQrAttendance::getCheckedInAt)));
    }
}
