package com.cozy.mall.mq;

import com.cozy.common.constant.InviteRewardConfig;
import com.cozy.common.mq.InviteRewardEarnedEvent;
import com.cozy.common.mq.WelcomeGiftEligibleEvent;
import com.cozy.mall.api.PointsMallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserLifecycleConsumersTest {

    private PointsMallService pointsMallService;
    private WelcomeGiftEligibleConsumer welcomeConsumer;
    private InviteRewardEarnedConsumer inviteConsumer;

    @BeforeEach
    void setUp() {
        pointsMallService = mock(PointsMallService.class);
        welcomeConsumer = new WelcomeGiftEligibleConsumer(pointsMallService);
        inviteConsumer = new InviteRewardEarnedConsumer(pointsMallService, new InviteRewardConfig());
    }

    @Test
    void welcomeGiftUsesDedicatedLegacyEquivalentEntry() {
        welcomeConsumer.onMessage(WelcomeGiftEligibleEvent.builder()
                .userId(38L).uniqueKey("NEW_USER_COUPON_38").occurredAt(LocalDateTime.now()).build());

        verify(pointsMallService).issueNewUserCoupon(38L);
    }

    @Test
    void inviteRewardKeepsRulesInMallAndTargetsInviter() {
        inviteConsumer.onMessage(InviteRewardEarnedEvent.builder()
                .inviteeUserId(38L).inviterId(7L).uniqueKey("invite_firstorder_38_7")
                .occurredAt(LocalDateTime.now()).build());

        verify(pointsMallService).issueCouponToUser(7L, "BOGO", "invite_firstorder_38_7", 0, 40, 30);
    }

    @Test
    void rejectsWelcomeGiftWithWrongLegacyKey() {
        assertThrows(IllegalArgumentException.class, () -> welcomeConsumer.onMessage(
                WelcomeGiftEligibleEvent.builder().userId(38L).uniqueKey("welcome_38")
                        .occurredAt(LocalDateTime.now()).build()));
    }

    @Test
    void rejectsInviteEventWithoutOccurrenceTime() {
        assertThrows(IllegalArgumentException.class, () -> inviteConsumer.onMessage(
                InviteRewardEarnedEvent.builder().inviteeUserId(38L).inviterId(7L)
                        .uniqueKey("invite_firstorder_38_7").build()));
    }
}
