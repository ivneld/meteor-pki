package io.autocrypt.vehicles.pki.x509;

import org.junit.jupiter.api.Tag;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * Spring 컨텍스트를 올리는 테스트의 공통 베이스. contextTest 태스크로 실행된다.
 */
@Tag("context")
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public abstract class ContextTest {

}
