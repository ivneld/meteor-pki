package io.autocrypt.vehicles.pki.x509;

import org.junit.jupiter.api.Tag;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

/**
 * CI 에서 돌리지 않는 개발용 테스트 베이스. developTest 태스크로만 실행된다.
 */
@Tag("develop")
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public abstract class DevelopTest {

}
