package br.com.fiap.clinic.history.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class TokenService {

    @Value("${api.security.token.public-key}")
    private String publicKeyContent;

    public DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.RSA256(getPublicKey(), null);
            return JWT.require(algorithm)
                    .withIssuer("clinicfiap-scheduler")
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception){
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar chave pública", e);
        }
    }

    private RSAPublicKey getPublicKey() throws Exception {
        String key = publicKeyContent.replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}