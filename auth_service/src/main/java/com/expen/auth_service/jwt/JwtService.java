package com.expen.auth_service.jwt;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
    
    public String getToken(UserDetails user, Long userId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        return getToken(extraClaims, user);
    }

    public String getToken(Map<String, Object> extraClaims, UserDetails user) {
        String token = Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();

        log.info("Token generado para el usuario: {}", user.getUsername());
        log.debug("Token generado: {}", token);
        return token;
    }

    private Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        log.debug("Clave secreta decodificada y convertida a Key: {}", key);
        return key;
    }

    public String extractUsername(String token) {
        String username = extractClaim(token, Claims::getSubject);
        log.info("Username extraído del token: {}", username);
        return username;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        T claimValue = claimsResolver.apply(claims);
        log.debug("Claim extraído del token: {}", claimValue);
        return claimValue;
    }

    private Claims extractAllClaims(String token) {
        try {
            Claims claims = Jwts
                    .parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.info("Token válido. Claims extraídos: {}", claims);
            return claims;
        } catch (Exception e) {
            log.error("Error al extraer claims del token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isUsernameValid = username.equals(userDetails.getUsername());
        boolean isTokenExpired = isTokenExpired(token);

        log.info("Validando token para el usuario: {}", username);
        log.debug("Username válido: {}", isUsernameValid);
        log.debug("Token expirado: {}", isTokenExpired);

        return isUsernameValid && !isTokenExpired;
    }

    private boolean isTokenExpired(String token) {
        Date expirationDate = extractExpiration(token);
        boolean isExpired = expirationDate.before(new Date());

        log.debug("Fecha de expiración del token: {}", expirationDate);
        log.debug("Token expirado: {}", isExpired);

        return isExpired;
    }

    private Date extractExpiration(String token) {
        Date expirationDate = extractClaim(token, Claims::getExpiration);
        log.debug("Fecha de expiración extraída del token: {}", expirationDate);
        return expirationDate;
    }

    public String generateResetToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateResetToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Token inválido o expirado");
        }
    }
}