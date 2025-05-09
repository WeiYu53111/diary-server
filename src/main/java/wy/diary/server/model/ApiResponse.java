package wy.diary.server.model;

import java.util.HashMap;
import java.util.Map;

public class ApiResponse {
    private String status;
    private String message;
    private Object data;

    private ApiResponse(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static Map<String, Object> success(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    public static Map<String, Object> success(Object data) {
        return success("操作成功", data);
    }

    public static Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("data", null);
        return response;
    }

    public static Map<String, Object> error(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    // Getters
    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}