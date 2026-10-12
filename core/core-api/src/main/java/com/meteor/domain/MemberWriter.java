package com.meteor.domain;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 생성과 상태 변경. 꺼내고, 애그리거트에 넘기고, 저장한다. 규칙 판단은 {@link Member} 가 한다(S-04).
 */
@Component
@Transactional
public class MemberWriter {

    private final MemberRepository memberRepository;

    public MemberWriter(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /** 생성 결과로 필요한 것이 식별자뿐이면 애그리거트 대신 식별자만 돌려준다. */
    public Long register(Email email, String name) {
        return memberRepository.save(Member.register(email, name)).getId();
    }

    public Member withdraw(Long memberId) {
        Member member = load(memberId);
        member.withdraw();
        return memberRepository.save(member);
    }

    private Member load(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", memberId));
    }

}
