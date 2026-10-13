package io.autocrypt.vehicles.pki.x509.api;

import io.autocrypt.vehicles.pki.x509.ContextTest;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 애플리케이션 전체(컨트롤러 → usecase → 도메인 → H2)를 HTTP 요청으로 검증하는 테스트의 베이스.
 */
public abstract class ApiIT extends ContextTest {

    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

}
