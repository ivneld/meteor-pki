package io.autocrypt.vehicles.pki.x509.support.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * L4/L7 헬스체크용 단순 엔드포인트. 상세 상태는 actuator 를 사용한다.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

}
