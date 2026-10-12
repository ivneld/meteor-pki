package com.meteor.domain;

import com.meteor.enums.MemberStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    void registeredMemberIsActive() {
        Member member = Member.register(Email.of("a@b.com"), "kim");

        assertThat(member.isNew()).isTrue();
        assertThat(member.isActive()).isTrue();
    }

    @Test
    void withdrawTwiceIsRejected() {
        Member member = Member.restore(1L, Email.of("a@b.com"), "kim", MemberStatus.ACTIVE);
        member.withdraw();

        assertThat(member.isActive()).isFalse();
        assertThatThrownBy(member::withdraw).isInstanceOf(CoreException.class);
    }

    @Test
    void withdrawnMemberFailsActiveCheck() {
        Member member = Member.restore(1L, Email.of("a@b.com"), "kim", MemberStatus.WITHDRAWN);

        assertThatThrownBy(member::ensureActive).isInstanceOf(CoreException.class)
            .extracting(e -> ((CoreException) e).getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_NOT_ACTIVE);
    }

}
