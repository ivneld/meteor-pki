package com.meteor.pki.usecase;

import java.util.List;

import com.meteor.pki.domain.CertProfile;
import com.meteor.pki.domain.CertProfileWriter;
import com.meteor.pki.domain.DistributionPoints;
import com.meteor.pki.domain.Eku;
import com.meteor.pki.domain.EkuReader;
import com.meteor.pki.domain.ProfileEku;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertProfileUseCase {

    private final EkuReader ekuReader;

    private final CertProfileWriter certProfileWriter;

    private final DistributionPoints distributionPoints;

    public CertProfileUseCase(EkuReader ekuReader, CertProfileWriter certProfileWriter,
            DistributionPoints distributionPoints) {
        this.ekuReader = ekuReader;
        this.certProfileWriter = certProfileWriter;
        this.distributionPoints = distributionPoints;
    }

    @Transactional
    public CertProfile create(CertProfileCreateCommand command) {
        List<ProfileEku> extendedKeyUsages = ekuReader.getAllByOids(command.ekuOids())
            .stream()
            .map(Eku::toProfileEku)
            .toList();
        return certProfileWriter.create(command.toDraft(extendedKeyUsages, distributionPoints.defaultOcspUri()));
    }

}
