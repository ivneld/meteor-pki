package com.meteor.api;

import com.meteor.api.request.MemberRegisterRequest;
import com.meteor.domain.MemberReader;
import com.meteor.domain.MemberWriter;
import com.meteor.support.web.ApiControllerAdvice;
import com.meteor.test.api.RestDocsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static com.meteor.support.docs.ProblemFields.problem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest extends RestDocsTest {

    private MemberWriter memberWriter;

    @BeforeEach
    void setUp() {
        memberWriter = mock(MemberWriter.class);
        mockMvc = mockController(new MemberController(mock(MemberReader.class), memberWriter),
                new ApiControllerAdvice());
    }

    @Test
    void register() throws Exception {
        when(memberWriter.register(any(), anyString())).thenReturn(1L);

        mockMvc
            .perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON)
                .content(json(new MemberRegisterRequest("kim@example.com", "kim"))))
            .andExpect(status().isOk())
            .andDo(document("member-register", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                            fieldWithPath("name").type(JsonFieldType.STRING).description("이름")),
                    responseFields(fieldWithPath("id").type(JsonFieldType.NUMBER).description("회원 ID"))));
    }

    @Test
    void registerWithInvalidEmail() throws Exception {
        // VO(Email) 생성 실패 → IllegalArgumentException → C001
        mockMvc
            .perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON)
                .content(json(new MemberRegisterRequest("not-an-email", "kim"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("C001"))
            .andDo(document("member-register-invalid-email", preprocessResponse(prettyPrint()), problem()));
    }

    private static String json(Object body) {
        return JsonMapper.builder().build().writeValueAsString(body);
    }

}
