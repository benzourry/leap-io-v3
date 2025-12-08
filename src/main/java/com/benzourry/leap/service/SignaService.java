package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.Signa;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.SignaRepository;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Optional;

@Service
public class SignaService {

    private final SignaRepository signaRepository;

    private final AppRepository appRepository;

    public SignaService(SignaRepository signaRepository,
                        AppRepository appRepository) {
        this.signaRepository = signaRepository;
        this.appRepository = appRepository;
    }

    public Signa save(Long appId,Signa signa, String email) {
        App app = appRepository.getReferenceById(appId);
        signa.setApp(app);
        signa.setEmail(email);
        return signaRepository.save(signa);
    }

    public Signa get(Long id) {
        return signaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Signa", "id", id));
    }

    public void delete(Long signaId) {
        signaRepository.deleteById(signaId);
    }

    public Page<Signa> getSignaList(Long appId, Pageable pageable) {
        return signaRepository.findByAppId(appId, pageable);
    }

    public Signa generateAndStoreKey(Long signaId) {

        Signa signa = get(signaId);
        try {
            // Ensure BC available
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            // Ensure directory exists
            String baseDir = Constant.UPLOAD_ROOT_DIR + "/attachment/signa-" + signa.getId() + "/";
            Files.createDirectories(Paths.get(baseDir));

            // ---- 1. Generate Key Pair ----
            KeyPair keyPair;

            if ("RSA".equalsIgnoreCase(signa.getKeyAlg())) {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
                kpg.initialize(2048);
                keyPair = kpg.generateKeyPair();

            } else if ("EC".equalsIgnoreCase(signa.getKeyAlg()) ||
                    "ECDSA".equalsIgnoreCase(signa.getKeyAlg())) {

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");

                // Default EC curve
                String curve = signa.getEcCurve() != null ? signa.getEcCurve() : "secp256r1";

                ECGenParameterSpec ecSpec = new ECGenParameterSpec(curve);
                kpg.initialize(ecSpec, new SecureRandom());

                keyPair = kpg.generateKeyPair();

            } else {
                throw new IllegalArgumentException("Unsupported keyAlg: " + signa.getKeyAlg());
            }

            // ---- 2. Generate Self Signed Certificate ----
            X509Certificate cert = generateSelfSignedCertificate(signa, keyPair);

            // ---- 3. Save in Keystore ----
            String fileName = signa.getName().replaceAll("[^a-zA-Z0-9_-]", "_")
                    + "_" + System.currentTimeMillis()
                    + (signa.getKeystoreType().equalsIgnoreCase("PKCS12") ? ".p12" : ".jks");

            String filePath = baseDir + fileName;

            KeyStore ks = KeyStore.getInstance(
                    signa.getKeystoreType().equalsIgnoreCase("PKCS12") ? "PKCS12" : "JKS"
            );

            ks.load(null, null);

            char[] pwd = (signa.getPassword() == null) ? new char[0] : signa.getPassword().toCharArray();

            ks.setKeyEntry(
                    "key",
                    keyPair.getPrivate(),
                    pwd,
                    new Certificate[]{cert}
            );

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                ks.store(fos, pwd);
            }

            // ---- 4. Update database ----
            signa.setKeyPath(fileName);
            signa.setHashAlg(signa.getHashAlg() == null ? "SHA256" : signa.getHashAlg());
            signa.setKeyAlg(signa.getKeyAlg() == null ? "RSA" : signa.getKeyAlg());

            return signaRepository.save(signa);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key", e);
        }
    }


    public Signa clearFile(Long signaId, String type) {
        Signa signa = get(signaId);
        if (signa == null) {
            throw new IllegalArgumentException("Signa cannot be null");
        }

        String baseDir = Constant.UPLOAD_ROOT_DIR+"/attachment/signa-" + signa.getId() + "/";

        String filePath = null;

        // No file to delete
        if ("key".equals(type)) {
            String keyPath = signa.getKeyPath();
            filePath = signa.getKeyPath();
            if (keyPath == null || keyPath.isBlank()) {
                signa.setKeyPath(null);
                return signaRepository.save(signa);
            }
        }

        if ("img".equals(type)) {
            String imgPath = signa.getImagePath();
            filePath = signa.getImagePath();
            if (imgPath == null || imgPath.isBlank()) {
                signa.setImagePath(null);
                return signaRepository.save(signa);
            }
        }

        Path path = Paths.get(baseDir+filePath);

        try {
            // Delete if exists
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete key file: " + filePath, e);
        }

        if ("key".equals(type)) {
            signa.setKeyPath(null);
        } else if ("img".equals(type)) {
            signa.setImagePath(null);
        }

        // Save to database
        return signaRepository.save(signa);
    }





    private X509Certificate generateSelfSignedCertificate(Signa signa, KeyPair keyPair) throws Exception {

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        String dn = "CN=" + signa.getName()
                + ", O=" + Optional.ofNullable(signa.getLocation()).orElse("Unknown")
                + ", C=MY";

        if (signa.getDn() != null && !signa.getDn().isEmpty()) {
            dn = signa.getDn();
        }

        X500Name issuer = new X500Name(dn);

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, issuer, keyPair.getPublic()
        );

        String hashAlg = signa.getHashAlg() == null ? "SHA256" : signa.getHashAlg();
        String keyAlg = signa.getKeyAlg() == null ? "RSA" : signa.getKeyAlg();

        ContentSigner signer = new JcaContentSignerBuilder(hashAlg+"with"+keyAlg).build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(builder.build(signer));
    }


    public String generateCSR(Signa signa) {
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            char[] pwd = (signa.getPassword() == null) ? new char[0] : signa.getPassword().toCharArray();

            KeyStore ks = KeyStore.getInstance(
                    signa.getKeystoreType().equalsIgnoreCase("PKCS12") ? "PKCS12" : "JKS"
            );
            ks.load(new FileInputStream(Constant.UPLOAD_ROOT_DIR + "/attachment/signa-" + signa.getId() + "/" + signa.getKeyPath()),
                    pwd);

            PrivateKey privateKey = (PrivateKey) ks.getKey("key", pwd);
            X509Certificate cert = (X509Certificate) ks.getCertificate("key");
            PublicKey publicKey = cert.getPublicKey();

            String dn = "CN=" + signa.getName()
                    + ", O=" + Optional.ofNullable(signa.getLocation()).orElse("Unknown")
                    + ", C=MY";

            if (signa.getDn() != null && !signa.getDn().isEmpty()) {
                dn = signa.getDn();
            }

            X500Name subject = new X500Name(dn);

            PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(subject, publicKey);

            String hashAlg = signa.getHashAlg() == null ? "SHA256" : signa.getHashAlg().toUpperCase();
            String keyAlg = signa.getKeyAlg() == null ? "RSA" : signa.getKeyAlg().toUpperCase();

            ContentSigner signer = new JcaContentSignerBuilder(hashAlg + "with" + keyAlg)
                    .setProvider("BC")
                    .build(privateKey);

            PKCS10CertificationRequest csr = csrBuilder.build(signer);

            StringWriter sw = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
                pemWriter.writeObject(csr);
            }

            return sw.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSR", e);
        }
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "Unknown" : s;
    }
}
