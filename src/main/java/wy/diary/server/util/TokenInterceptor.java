package wy.diary.server.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    
    // 管理员 Token 常量，可以考虑从配置文件中读取
    @Value("${android.appid}")
    private String ADMIN_TOKEN;
    
    // 管理员标识
    private static final String ADMIN_ID = "admin";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Authorization header is missing or invalid.");
            return false;
        }

        String token = authHeader.substring(7);
        
        // 检查是否是管理员 token
        if (ADMIN_TOKEN.equals(token)) {
            // 设置管理员标识
            request.setAttribute("openid", ADMIN_TOKEN);
            request.setAttribute("isAdmin", true);
            return true;
        }
        
        // 普通用户 token 验证
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid token.");
            return false;
        }

        String openid = jwtUtil.getOpenidFromToken(token);
        request.setAttribute("openid", openid);
        request.setAttribute("isAdmin", false);
        return true;
    }
}
