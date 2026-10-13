package io.autocrypt.vehicles.pki.x509.domain;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;

/**
 * 프로파일이 발급하는 인증서에 넣을 주체 대체 이름(SAN). {@code TYPE:VALUE} 를 쉼표나 세미콜론으로 이은 문자열이다(예:
 * {@code DNS:ra.example.com,IP:10.0.0.1}). 타입은 DNS, IP, EMAIL(RFC822), URI 이다. 빈 문자열은 SAN
 * 이 없다는 뜻이다.
 */
public record SubjectAltNameTemplate(String csv) {

    public static final SubjectAltNameTemplate EMPTY = new SubjectAltNameTemplate("");

    private static final int MAX_LENGTH = 2000;

    private static final Pattern IPV4 = Pattern
        .compile("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    private static final Pattern DNS_LABEL = Pattern.compile("^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?$");

    public SubjectAltNameTemplate {
        csv = csv == null ? "" : csv;
        if (!csv.isBlank()) {
            parse(csv);
        }
    }

    /** 입력을 검증하고 쉼표로 다시 이어 저장 형태로 만든다. 없거나 비면 {@link #EMPTY}. */
    public static SubjectAltNameTemplate of(String text) {
        if (text == null || text.isBlank()) {
            return EMPTY;
        }
        parse(text);
        String normalized = String.join(",",
                Arrays.stream(text.split(",")).map(String::trim).filter(token -> !token.isEmpty()).toList());
        if (normalized.length() > MAX_LENGTH) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "SubjectAltName CSV length must be <= 2000");
        }
        return new SubjectAltNameTemplate(normalized);
    }

    public boolean isEmpty() {
        return csv.isBlank();
    }

    public List<Entry> entries() {
        return isEmpty() ? List.of() : parse(csv);
    }

    public GeneralNames toGeneralNames() {
        return new GeneralNames(entries().stream().map(Entry::toGeneralName).toArray(GeneralName[]::new));
    }

    /** SAN 항목 하나. type 은 대문자. */
    public record Entry(String type, String value) {

        GeneralName toGeneralName() {
            return switch (type) {
                case "DNS" -> new GeneralName(GeneralName.dNSName, value);
                case "IP" -> new GeneralName(GeneralName.iPAddress, value);
                case "EMAIL", "RFC822" -> new GeneralName(GeneralName.rfc822Name, value);
                default -> new GeneralName(GeneralName.uniformResourceIdentifier, value);
            };
        }

    }

    private static List<Entry> parse(String text) {
        List<Entry> entries = new ArrayList<>();
        for (String raw : text.split("[;,]")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            int colon = token.indexOf(':');
            if (colon <= 0) {
                throw invalid("Each SubjectAltName must be TYPE:VALUE");
            }
            String type = token.substring(0, colon).trim().toUpperCase(Locale.ROOT);
            String value = token.substring(colon + 1).trim();
            if (value.isEmpty()) {
                throw invalid("SubjectAltName value is empty.");
            }
            validate(type, value);
            entries.add(new Entry(type, value));
        }
        if (entries.isEmpty()) {
            throw invalid("SubjectAltName requires at least one entry");
        }
        return entries;
    }

    private static void validate(String type, String value) {
        switch (type) {
            case "DNS" -> {
                if (!isDnsName(value)) {
                    throw invalid("Invalid DNS name: " + value);
                }
            }
            case "IP" -> {
                if (!isIpAddress(value)) {
                    throw invalid("Invalid IP address: " + value);
                }
            }
            case "EMAIL", "RFC822" -> {
                int at = value.lastIndexOf('@');
                if (at <= 0 || !isDnsName(value.substring(at + 1))) {
                    throw invalid("Invalid email address: " + value);
                }
            }
            case "URI" -> {
                try {
                    new URI(value);
                }
                catch (URISyntaxException ex) {
                    throw invalid("Invalid URI: " + value);
                }
            }
            default -> throw invalid("Unsupported SubjectAltName type: " + type);
        }
    }

    private static boolean isDnsName(String name) {
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(name)) {
            return false;
        }
        String host = name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
        if (host.isEmpty() || host.length() > 253) {
            return false;
        }
        String[] labels = host.split("\\.", -1);
        for (int i = 0; i < labels.length; i++) {
            boolean wildcard = i == 0 && labels[i].equals("*") && labels.length >= 2;
            if (!wildcard && !DNS_LABEL.matcher(labels[i]).matches()) {
                return false;
            }
        }
        String tld = labels[labels.length - 1];
        return labels.length == 1 || tld.length() >= 2;
    }

    private static boolean isIpAddress(String value) {
        if (IPV4.matcher(value).matches()) {
            return true;
        }
        if (!value.contains(":")) {
            return false;
        }
        try {
            return InetAddress.getByName(value) instanceof Inet6Address;
        }
        catch (UnknownHostException ex) {
            return false;
        }
    }

    private static CoreException invalid(String message) {
        return new CoreException(ErrorCode.CERT_INVALID_INPUT, message);
    }

}
