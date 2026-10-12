package com.meteor.api.response;

import com.meteor.domain.Member;
import com.meteor.enums.MemberStatus;

/**
 * 응답 DTO. 값 객체는 원시 타입으로 풀고 상태는 core-enum 의 enum 을 그대로 쓴다(R-12). 애그리거트에서는 getter 만
 * 읽는다(R-10).
 */
public record MemberResponse(Long id, String email, String name, MemberStatus status) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail().value(), member.getName(), member.getStatus());
    }

}
