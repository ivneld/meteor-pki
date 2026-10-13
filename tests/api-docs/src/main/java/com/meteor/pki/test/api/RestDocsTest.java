package com.meteor.pki.test.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsTest {

    protected MockMvc mockMvc;

    private RestDocumentationContextProvider restDocumentation;

    @BeforeEach
    void setUpRestDocumentation(RestDocumentationContextProvider restDocumentation) {
        this.restDocumentation = restDocumentation;
    }

    protected MockMvc mockController(Object controller, Object... controllerAdvices) {
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(controllerAdvices)
            .apply(MockMvcRestDocumentation.documentationConfiguration(this.restDocumentation))
            .build();
    }

}
