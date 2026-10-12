package io.autocrypt.vehicles.pki.x509.test.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 컨트롤러를 standalone 으로 띄워 REST Docs 스니펫을 만드는 테스트의 베이스. Spring 컨텍스트를 올리지 않는다.
 */
@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsTest {

    protected MockMvc mockMvc;

    private RestDocumentationContextProvider restDocumentation;

    @BeforeEach
    void setUpRestDocumentation(RestDocumentationContextProvider restDocumentation) {
        this.restDocumentation = restDocumentation;
    }

    /**
     * @param controller 문서화할 컨트롤러
     * @param controllerAdvices 오류 응답까지 문서화하려면 {@code @ControllerAdvice} 인스턴스를 함께 넘긴다
     */
    protected MockMvc mockController(Object controller, Object... controllerAdvices) {
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(controllerAdvices)
            .apply(MockMvcRestDocumentation.documentationConfiguration(this.restDocumentation))
            .build();
    }

}
