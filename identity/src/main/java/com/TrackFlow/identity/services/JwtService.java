package com.TrackFlow.identity.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {


    private final SecretKey key;
    private final long expirationMs;


    public JwtService( @Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-ms:3600000}") long expirationMs){
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token){
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        Claims claims = extractClaims(token);
        return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date());
    }

    public Instant extractExpirationInstant(String token) {
        return extractClaims(token).getExpiration().toInstant();
    }

    public long getExpirationInSeconds() {
        return expirationMs / 1000;
    }

    private Claims extractClaims(String token){
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
