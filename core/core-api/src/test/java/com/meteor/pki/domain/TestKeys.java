package com.meteor.pki.domain;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import com.meteor.pki.enums.SignatureAlgorithm;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

public final class TestKeys {

    private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

    private TestKeys() {
    }

    public static KeyPair ecP256() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", PROVIDER);
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static SubjectPublicKey publicKey(KeyPair keyPair) {
        return new SubjectPublicKey(keyPair.getPublic().getEncoded());
    }

    public static byte[] sign(TbsCertificate tbs, KeyPair signer) {
        try {
            Signature signature = Signature.getInstance(tbs.signatureAlgorithm().getJcaName(), PROVIDER);
            signature.initSign(signer.getPrivate());
            signature.update(tbs.toBeSigned());
            return signature.sign();
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String csrPem(String subjectDn, KeyPair keyPair, String... dnsNames) {
        try {
            JcaPKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Name(subjectDn), keyPair.getPublic());
            if (dnsNames.length > 0) {
                ExtensionsGenerator extensions = new ExtensionsGenerator();
                extensions.addExtension(Extension.subjectAlternativeName, false,
                        new GeneralNames(java.util.Arrays.stream(dnsNames)
                            .map(name -> new GeneralName(GeneralName.dNSName, name))
                            .toArray(GeneralName[]::new)));
                builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions.generate());
            }
            byte[] der = builder
                .build(new JcaContentSignerBuilder("SHA256withECDSA").setProvider(PROVIDER).build(keyPair.getPrivate()))
                .getEncoded();
            return "-----BEGIN CERTIFICATE REQUEST-----\n" + Base64.getMimeEncoder().encodeToString(der)
                    + "\n-----END CERTIFICATE REQUEST-----\n";
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static SignatureAlgorithm ecdsa() {
        return SignatureAlgorithm.SHA256_WITH_ECDSA;
    }

}
