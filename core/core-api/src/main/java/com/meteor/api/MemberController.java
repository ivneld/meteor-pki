package com.meteor.api;

import com.meteor.api.request.MemberRegisterRequest;
import com.meteor.api.response.MemberRegisteredResponse;
import com.meteor.api.response.MemberResponse;
import com.meteor.domain.MemberReader;
import com.meteor.domain.MemberWriter;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 API. 모든 흐름이 회원 애그리거트 하나로 끝나므로 usecase 없이 도메인 서비스를 바로 부른다.
 */
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberReader memberReader;

    private final MemberWriter memberWriter;

    public MemberController(MemberReader memberReader, MemberWriter memberWriter) {
        this.memberReader = memberReader;
        this.memberWriter = memberWriter;
    }

    @PostMapping
    public MemberRegisteredResponse register(@RequestBody @Valid MemberRegisterRequest request) {
        return new MemberRegisteredResponse(memberWriter.register(request.toEmail(), request.name()));
    }

    @GetMapping("/{memberId}")
    public MemberResponse get(@PathVariable Long memberId) {
        return MemberResponse.from(memberReader.find(memberId));
    }

    @PostMapping("/{memberId}/withdraw")
    public MemberResponse withdraw(@PathVariable Long memberId) {
        return MemberResponse.from(memberWriter.withdraw(memberId));
    }

}
