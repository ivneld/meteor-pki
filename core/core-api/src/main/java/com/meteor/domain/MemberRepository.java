package com.meteor.domain;

import java.util.Optional;

import com.meteor.storage.MemberEntity;
import com.meteor.storage.MemberJpaRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * 회원 저장소 어댑터. storage 모듈의 JPA 엔티티를 애그리거트로 바꾸고, 그 반대도 한다. 엔티티는 이 클래스 밖으로 나가지 않는다(R-05).
 *
 * <p>
 * 더티 체킹에 기대지 않는다. 애그리거트를 바꿨으면 {@link #save(Member)} 를 명시적으로 호출해야 한다(S-05).
 */
@Repository
public class MemberRepository {

    private final MemberJpaRepository jpa;

    MemberRepository(MemberJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Member> findById(Long id) {
        return jpa.findById(id).map(MemberRepository::toDomain);
    }

    public Member save(Member member) {
        MemberEntity entity;
        if (member.isNew()) {
            entity = new MemberEntity(member.getEmail().value(), member.getName(), member.getStatus());
        }
        else {
            entity = jpa.findById(member.getId())
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", member.getId()));
            entity.changeStatus(member.getStatus());
        }
        return toDomain(jpa.save(entity));
    }

    private static Member toDomain(MemberEntity entity) {
        return Member.restore(entity.getId(), Email.of(entity.getEmail()), entity.getName(), entity.getStatus());
    }

}
