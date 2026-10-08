package dev.ruancmm.gerenciador_tarefas.auth;

import java.sql.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import dev.ruancmm.gerenciador_tarefas.auth.exception.InvalidTokenException;

@Component
public class JwtUtil {

    private SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expiration}") long accessExpiration,
        @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(secret)
        );
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(Long userId) {
        return generateToken(userId, "access", accessExpiration);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, "refresh", refreshExpiration);
    }

    private String generateToken(Long userId, String type, long duration) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("type", type)
            .expiration(new Date(System.currentTimeMillis() + duration))
            .signWith(key)
            .compact();
    }

    public Long extractUserId(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!expectedType.equals(claims.get("type", String.class))) {
                throw new InvalidTokenException("Invalid token type");
            }

            if (claims.getSubject() == null) {
                throw new InvalidTokenException("Token without userId");
            }

            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidTokenException("Invalid userId in token");
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token expired");
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid Token");
        }
    }
}