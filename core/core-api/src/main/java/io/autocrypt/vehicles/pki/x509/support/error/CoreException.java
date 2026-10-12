package io.autocrypt.vehicles.pki.x509.support.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * 도메인·애플리케이션·저장소·클라이언트 어디서든 던질 수 있는 유일한 예외 타입. 무엇이 잘못됐는지, 어떤 HTTP 상태로 응답할지는
 * {@link ErrorCode} 가 결정한다.
 *
 * <pre>
 * throw new CoreException(ErrorCode.RESOURCE_NOT_FOUND).property("akiHex", akiHex);
 * throw new CoreException(ErrorCode.INVALID_INPUT, "name must be shorter than 100 characters");
 * </pre>
 */
public class CoreException extends RuntimeException {

    private final ErrorCode errorCode;

    private final String detail;

    /** RFC 9457 표준 필드. 확장 속성이 이 이름을 쓰면 응답에서 표준 필드를 덮어쓰므로 거부한다. */
    private static final Set<String> RESERVED = Set.of("type", "title", "status", "detail", "instance");

    private final Map<String, Object> properties = new LinkedHashMap<>();

    public CoreException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    /**
     * @param detail 발생 건에 대한 부가 설명. 응답의 detail 로 그대로 내려간다. 없으면 null.
     */
    public CoreException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getTitle());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * 응답에 실릴 확장 속성을 추가한다. 응답 JSON 최상위에 그대로 노출되므로 민감 정보는 넣지 않는다.
     */
    public CoreException property(String name, Object value) {
        if (RESERVED.contains(name)) {
            throw new IllegalArgumentException("reserved problem detail field: " + name);
        }
        this.properties.put(name, value);
        return this;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

}
