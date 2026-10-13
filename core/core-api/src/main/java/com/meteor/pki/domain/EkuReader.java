package com.meteor.pki.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
