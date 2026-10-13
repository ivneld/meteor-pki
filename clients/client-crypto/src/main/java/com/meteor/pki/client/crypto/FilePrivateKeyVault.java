package com.meteor.pki.client.crypto;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.security.Provider;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.openssl.PKCS8Generator;

class FilePrivateKeyVault implements PrivateKeyVault {

    private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    private final Path directory;

    private final char[] password;

    private final Provider provider;

    FilePrivateKeyVault(Path directory, char[] password, Provider provider) {
        this.directory = directory;
        this.password = password.clone();
        this.provider = provider;
        try {
            Files.createDirectories(directory);
        }
        catch (IOException ex) {
            throw new CryptoClientException("Cannot create key directory: " + directory, ex);
        }
    }

    @Override
    public synchronized void store(String keyId, PrivateKey privateKey) {
        Path file = fileOf(keyId);
        if (Files.exists(file)) {
            throw new CryptoClientException("Key id already exists: " + keyId);
        }
        Path temp = directory.resolve(keyId + ".p8.tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.US_ASCII, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING); JcaPEMWriter pem = new JcaPEMWriter(writer)) {
            pem.writeObject(new PKCS8Generator(PrivateKeyInfo.getInstance(privateKey.getEncoded()),
                    new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC).setProvider(provider)
                        .setPassword(password)
                        .build()));
        }
        catch (IOException | OperatorCreationException ex) {
            throw new CryptoClientException("Cannot store key: " + keyId, ex);
        }
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException ex) {
            throw new CryptoClientException("Cannot store key: " + keyId, ex);
        }
    }

    @Override
    public PrivateKey load(String keyId) {
        Path file = fileOf(keyId);
        if (!Files.exists(file)) {
            throw new CryptoClientException("Key not found: " + keyId);
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.US_ASCII);
                PEMParser parser = new PEMParser(reader)) {
            PKCS8EncryptedPrivateKeyInfo encrypted = (PKCS8EncryptedPrivateKeyInfo) parser.readObject();
            PrivateKeyInfo info = encrypted.decryptPrivateKeyInfo(
                    new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider(provider).build(password));
            return new JcaPEMKeyConverter().setProvider(provider).getPrivateKey(info);
        }
        catch (IOException | OperatorCreationException | PKCSException | ClassCastException ex) {
            throw new CryptoClientException("Cannot load key: " + keyId, ex);
        }
    }

    private Path fileOf(String keyId) {
        if (keyId == null || !KEY_ID.matcher(keyId).matches()) {
            throw new CryptoClientException("Invalid key id: " + keyId);
        }
        return directory.resolve(keyId + ".p8");
    }

}
