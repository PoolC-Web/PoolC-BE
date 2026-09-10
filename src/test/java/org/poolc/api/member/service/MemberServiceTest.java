package org.poolc.api.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.poolc.api.activity.service.ActivityService;
import org.poolc.api.auth.infra.PasswordHashProvider;
import org.poolc.api.common.exception.ConflictException;
import org.poolc.api.member.domain.Member;
import org.poolc.api.member.domain.MemberRole;
import org.poolc.api.member.domain.MemberRoles;
import org.poolc.api.member.repository.MemberQueryRepository;
import org.poolc.api.member.repository.MemberRepository;
import org.poolc.api.poolc.service.PoolcService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock private MemberRepository memberRepository;
    @Mock private PasswordHashProvider passwordHashProvider;
    @Mock private MemberQueryRepository memberQueryRepository;
    @Mock private ActivityService activityService;
    @Mock private MemberResponseAssembler memberResponseAssembler;
    @Mock private PoolcService poolcService;

    @Test
    void deletesOnlyAnUnacceptedMember() {
        Member member = member("pending", MemberRole.UNACCEPTED);
        when(memberRepository.findByLoginID("pending")).thenReturn(Optional.of(member));

        service().deleteMember("pending");

        verify(memberRepository).delete(member);
    }

    @Test
    void rejectsHardDeleteForAnAcceptedMember() {
        Member member = member("member", MemberRole.MEMBER);
        when(memberRepository.findByLoginID("member")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service().deleteMember("member"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("승인 완료 회원은 삭제할 수 없습니다.");

        verify(memberRepository, never()).delete(member);
    }

    private MemberService service() {
        return new MemberService(memberRepository, passwordHashProvider, memberQueryRepository, activityService,
                memberResponseAssembler, poolcService);
    }

    private Member member(String loginId, MemberRole role) {
        return Member.builder()
                .UUID(loginId + "-uuid")
                .loginID(loginId)
                .passwordHash("password")
                .email(loginId + "@poolc.org")
                .phoneNumber("010-0000-0000")
                .name(loginId)
                .department("컴퓨터과학과")
                .studentID("2026000000")
                .roles(MemberRoles.getDefaultFor(role))
                .build();
    }
}
