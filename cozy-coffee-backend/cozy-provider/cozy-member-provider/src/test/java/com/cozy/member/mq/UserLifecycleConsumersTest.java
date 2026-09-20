package com.cozy.member.mq;

import com.cozy.common.constant.ProfileRewardConfig;
import com.cozy.common.mq.BirthdaySetEvent;
import com.cozy.common.mq.ProfileCompletedEvent;
import com.cozy.common.mq.UserCreatedEvent;
import com.cozy.member.api.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserLifecycleConsumersTest {

    private MemberService memberService;
    private UserCreatedConsumer userCreatedConsumer;
    private ProfileCompletedConsumer profileCompletedConsumer;
    private BirthdaySetConsumer birthdaySetConsumer;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        ProfileRewardConfig config = new ProfileRewardConfig();
        userCreatedConsumer = new UserCreatedConsumer(memberService);
        profileCompletedConsumer = new ProfileCompletedConsumer(memberService, config);
        birthdaySetConsumer = new BirthdaySetConsumer(memberService);
    }

    @Test
    void userCreatedUsesMemberDomainIdempotentEntry() {
        userCreatedConsumer.onMessage(UserCreatedEvent.builder()
                .userId(38L).uniqueKey("user_created_38").occurredAt(LocalDateTime.now()).build());

        verify(memberService).createMember(38L);
    }

    @Test
    void profileCompletedUsesUserIdAsDatabaseSourceId() {
        profileCompletedConsumer.onMessage(ProfileCompletedEvent.builder()
                .userId(38L).uniqueKey("profile_completed_38").occurredAt(LocalDateTime.now()).build());

        verify(memberService).addPointsWithLot(38L, 20, "profile", 38L,
                "完善个人资料（手机号+邮箱）奖励");
    }

    @Test
    void birthdayUsesBenefitYearFromEventInsteadOfCurrentClock() {
        birthdaySetConsumer.onMessage(BirthdaySetEvent.builder()
                .userId(38L).benefitYear(2025).uniqueKey("birthday_38_2025")
                .occurredAt(LocalDateTime.of(2025, 12, 31, 23, 59)).build());

        verify(memberService).grantBirthdayReward(38L, 2025);
    }

    @Test
    void rejectsMismatchedBusinessKey() {
        assertThrows(IllegalArgumentException.class, () -> profileCompletedConsumer.onMessage(
                ProfileCompletedEvent.builder().userId(38L).uniqueKey("profile_completed_39")
                        .occurredAt(LocalDateTime.now()).build()));
    }

    @Test
    void rethrowsBusinessFailureSoRocketMqCanRedeliver() {
        doThrow(new RuntimeException("db down")).when(memberService).createMember(anyLong());

        assertThrows(IllegalStateException.class, () -> userCreatedConsumer.onMessage(
                UserCreatedEvent.builder().userId(38L).uniqueKey("user_created_38")
                        .occurredAt(LocalDateTime.now()).build()));
    }
}
