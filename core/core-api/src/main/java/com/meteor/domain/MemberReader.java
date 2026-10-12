package com.meteor.domain;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 조회. 읽기 전용 트랜잭션에서 돌고, 호출자에게 트랜잭션이 있으면 거기에 참여한다(R-07).
 */
@Component
@Transactional(readOnly = true)
public class MemberReader {

    private final MemberRepository memberRepository;

    public MemberReader(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member find(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", memberId));
    }

    /** 활성 회원이 아니면 거절한다. 판단은 애그리거트({@link Member#ensureActive()})가 한다. */
    public void ensureActive(Long memberId) {
        find(memberId).ensureActive();
    }

}
