package wy.diary.server.util;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Encoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(String openid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("openid", openid);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public String getOpenidFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody()
                .get("openid", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        // 生成 HS512 密钥（自动满足 512 位要求）
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);

        // 获取密钥的字符串形式（Base64 编码）
        String secretKey = Encoders.BASE64.encode(key.getEncoded());
        System.out.println("Generated HS512 Key: " + secretKey);
    }
}
