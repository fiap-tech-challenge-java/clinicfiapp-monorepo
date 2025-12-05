package br.com.fiap.clinic.scheduler.config.security;

import br.com.fiap.clinic.scheduler.domain.entity.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.time.Instant;

@Service
public class TokenService {

    @Value("${api.security.token.private-key}")
    private String privateKeyContent;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.RSA256(null, getPrivateKey());

            return JWT.create()
                    .withIssuer("clinicfiap-scheduler")
                    .withSubject(user.getLogin())
                    .withClaim("userId", user.getId().toString())
                    .withClaim("role", user.getRole().name())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception){
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar chave privada", e);
        }
    }

    private Instant genExpirationDate(){
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }

    private RSAPrivateKey getPrivateKey() throws Exception {
        String key = privateKeyContent.replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    public String validateToken(String token) {
        return null;
    }
}