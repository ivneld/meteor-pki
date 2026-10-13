package com.meteor.pki.support.docs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

public final class ProblemFields {

    private ProblemFields() {
    }

    public static ResponseFieldsSnippet problem(FieldDescriptor... extensions) {
        List<FieldDescriptor> fields = new ArrayList<>(
                List.of(fieldWithPath("type").type(JsonFieldType.STRING).optional().description("about:blank"),
                        fieldWithPath("title").type(JsonFieldType.STRING).description("항상 PKI-ERROR"),
                        fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                        fieldWithPath("detail").type(JsonFieldType.STRING).description("상세 설명"),
                        fieldWithPath("instance").type(JsonFieldType.STRING).description("요청 경로"),
                        fieldWithPath("errorCode").type(JsonFieldType.STRING).description("에러 코드"),
                        fieldWithPath("timestamp").type(JsonFieldType.STRING).description("발생 시각(ISO-8601)")));
        fields.addAll(List.of(extensions));
        return responseFields(fields);
    }

}
