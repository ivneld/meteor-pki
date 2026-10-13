package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 등록된 EKU 조회.
 */
@Component
@Transactional(readOnly = true)
public class EkuReader {

    private final EkuRepository ekuRepository;

    public EkuReader(EkuRepository ekuRepository) {
        this.ekuRepository = ekuRepository;
    }

    public List<Eku> findAll() {
        return ekuRepository.findAll();
    }

    /** 요청한 순서대로 돌려준다. 하나라도 등록되지 않았으면 거절한다. */
    public List<Eku> getAllByOids(List<String> oids) {
        Map<String, Eku> registered = ekuRepository.findAllByOids(oids)
            .stream()
            .collect(Collectors.toMap(Eku::getOid, Function.identity()));
        return oids.stream().map(oid -> {
            Eku eku = registered.get(oid);
            if (eku == null) {
                throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT, "Unknown EKU OID.").property("oid", oid);
            }
            return eku;
        }).toList();
    }

}
