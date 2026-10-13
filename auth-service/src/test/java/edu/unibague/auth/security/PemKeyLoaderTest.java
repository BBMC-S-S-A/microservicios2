package edu.unibague.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PemKeyLoaderTest {

    @Test
    void loadsMatchingRsaKeyPair() throws Exception {
        RSAPublicKey publicKey = PemKeyLoader.readPublicKey(new ClassPathResource("keys/public.pem"));
        RSAPrivateKey privateKey = PemKeyLoader.readPrivateKey(new ClassPathResource("keys/private.pem"));

        byte[] data = "taller02".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(data);
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(data);
        assertThat(verifier.verify(signature)).isTrue();
    }
}
