package wy.diary.server.controller;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import wy.diary.server.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wx")
public class WxAuthController {

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    @Value("${wx.jscode2session.url}")
    private String jscode2sessionUrl;

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public WxAuthController(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) throws Exception {

        // 获取前端传来的code
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code不能为空");
        }
        // 调用微信接口获取openid和session_key
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                jscode2sessionUrl, appid, secret, code);
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                Map<String, String> result = objectMapper.readValue(responseBody, Map.class);
                
                if (result.containsKey("errcode")) {
                    throw new RuntimeException("微信接口调用失败: " + result.get("errmsg"));
                }

                String openid = result.get("openid");
                String sessionKey = result.get("session_key");

                // 生成JWT token
                String token = jwtUtil.generateToken(openid);

                // 返回结果
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("token", token);
                responseMap.put("openid", openid);
                return responseMap;
            }
        }
    }

    @PostMapping("/validate")
    public Map<String, Object> validateToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("token不能为空");
        }

        boolean isValid = jwtUtil.validateToken(token);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        
        if (isValid) {
            String openid = jwtUtil.getOpenidFromToken(token);
            response.put("openid", openid);
        }
        
        return response;
    }
}
