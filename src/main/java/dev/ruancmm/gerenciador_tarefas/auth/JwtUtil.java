package dev.ruancmm.gerenciador_tarefas.auth;

import java.sql.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import dev.ruancmm.gerenciador_tarefas.auth.exception.InvalidTokenException;

@Component
public class JwtUtil {

    private static final SecretKey KEY = Jwts.SIG.HS256.key().build();
    private static final long ACCESS_EXP = 900_000;
    private static final long REFRESH_EXP = 2_592_000_000L;

    public String generateAccessToken(Long userId) {
        return generateToken(userId, "access", ACCESS_EXP);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, "refresh", REFRESH_EXP);
    }

    private String generateToken(Long userId, String type, long duration) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("type", type)
            .expiration(new Date(System.currentTimeMillis() + duration))
            .signWith(KEY)
            .compact();
    }

    public Long extractUserId(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(KEY)
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