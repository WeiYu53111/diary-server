package wy.diary.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wy.diary.server.model.ApiResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Value("${image.storage.path}")
    private String storagePath;

    @PostMapping("/upload")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file, 
                                          @RequestParam("diaryId") String diaryId,
                                          @RequestAttribute("openid") String openid) throws IOException {
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("status", "error");
            response.put("message", "上传的文件为空");
            return response;
        }
        
        try {
            // 创建用户目录结构: /diary-server/images/{userId}/{year}/
            String year = String.valueOf(Year.now().getValue());
            String userDir = storagePath + openid + "/" + year + "/";

            // 确保目录存在
            Path dirPath = Paths.get(userDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 生成带日期时间的唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueFileName = timestamp + "_" + diaryId + fileExtension;

            // 保存文件
            Path filePath = Paths.get(userDir + uniqueFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath);
            }
            
            // 构建成功响应
            response.put("status", "success");
            response.put("message", "文件上传成功");
            response.put("data", new HashMap<String, String>() {{
                put("url", filePath.toString());
                put("fileName", uniqueFileName);
            }});
            
        } catch (IOException e) {
            // 构建错误响应
            response.put("status", "error");
            response.put("message", "文件上传失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 删除图片接口
     * @param requestData 请求体，包含图片URL列表和日记ID
     * @param openid 用户OpenID (由拦截器注入)
     * @return 删除结果
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteImages(@RequestBody Map<String, Object> requestData,
                                          @RequestAttribute("openid") String openid) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 从请求体中获取urls和diaryId
            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) requestData.get("urls");
            String diaryId = (String) requestData.get("diaryId");
            
            if (imageUrls == null || imageUrls.isEmpty()) {
                return ApiResponse.error("没有提供要删除的图片URLs");
            }
            
            if (diaryId == null || diaryId.isEmpty()) {
                return ApiResponse.error("没有提供日记ID");
            }
            
            int successCount = 0;
            int failCount = 0;
            Map<String, String> results = new HashMap<>();
            
            for (String url : imageUrls) {
                try {
                    Path filePath = Paths.get(url);
                    // 安全检查：确保要删除的文件属于当前用户
                    if (!filePath.toString().contains(openid) || !filePath.toString().contains(diaryId)) {
                        results.put(url, "权限不足，无法删除");
                        failCount++;
                        continue;
                    }
                    
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        results.put(url, "删除成功");
                        successCount++;
                    } else {
                        results.put(url, "文件不存在");
                        failCount++;
                    }
                } catch (Exception e) {
                    results.put(url, "删除失败: " + e.getMessage());
                    failCount++;
                }
            }
            
            // 构建数据响应
            Map<String, Object> data = new HashMap<>();
            data.put("results", results);
            data.put("successCount", successCount);
            data.put("failCount", failCount);
            data.put("diaryId", diaryId);
            
            return ApiResponse.success(String.format("成功删除 %d 张图片，失败 %d 张", successCount, failCount), data);
            
        } catch (Exception e) {
            return ApiResponse.error("删除图片失败: " + e.getMessage());
        }
    }
}
