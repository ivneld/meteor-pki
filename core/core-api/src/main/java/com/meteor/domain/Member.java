package com.meteor.domain;

import com.meteor.enums.MemberStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

/**
 * 회원 애그리거트. 다른 애그리거트는 이 객체를 필드로 갖지 않고 memberId 로만 참조한다.
 */
public class Member {

    private final Long id;

    private final Email email;

    private final String name;

    private MemberStatus status;

    private Member(Long id, Email email, String name, MemberStatus status) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.status = status;
    }

    public static Member register(Email email, String name) {
        return new Member(null, email, name, MemberStatus.ACTIVE);
    }

    /** 저장소에서 복원한다. MemberRepository 만 호출한다. */
    public static Member restore(Long id, Email email, String name, MemberStatus status) {
        return new Member(id, email, name, status);
    }

    public void withdraw() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new CoreException(ErrorCode.MEMBER_ALREADY_WITHDRAWN).property("memberId", id);
        }
        status = MemberStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    /** "이 회원으로 무언가를 해도 되는가". 판단은 회원이 하고 거절은 예외로 알린다. */
    public void ensureActive() {
        if (!isActive()) {
            throw new CoreException(ErrorCode.MEMBER_NOT_ACTIVE).property("memberId", id);
        }
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public MemberStatus getStatus() {
        return status;
    }

}
