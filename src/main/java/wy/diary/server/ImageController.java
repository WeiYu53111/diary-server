package wy.diary.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    /**
     * 获取图片接口
     * @param fileName 图片文件名
     * @param openid 用户OpenID (由拦截器注入)
     * @return 图片数据流
     */
    @GetMapping("/view")
    public ResponseEntity<byte[]> getImage(@RequestParam("file") String fileName,
                                          @RequestParam("id") String openid) {
        try {
            // 从文件名中提取年份（假设文件名格式为：yyyyMMdd_HHmmss_diaryId.ext）
            String year = ""; 
            if (fileName.length() >= 8) {
                year = fileName.substring(0, 4); // 从文件名中获取年份
            } else {
                // 若文件名不符合预期格式，使用当前年份
                year = String.valueOf(Year.now().getValue());
            }
            
            // 构建完整的文件路径
            String imagePath = storagePath + openid + "/" + year + "/" + fileName;
            Path filePath = Paths.get(imagePath);
            
            // 如果文件不存在，直接返回404错误
            if (!Files.exists(filePath)) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("图片不存在".getBytes());
            }
            
            // 读取图片数据
            byte[] imageData = Files.readAllBytes(filePath);
            
            // 确定图片的 MIME 类型
            String mimeType = determineContentType(fileName);
            
            // 返回带有适当内容类型的响应
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .contentLength(imageData.length)
                    .body(imageData);
            
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("获取图片失败: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 根据文件扩展名确定内容类型
     * @param filePath 文件路径
     * @return MIME类型
     */
    private String determineContentType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            default:
                return "application/octet-stream";
        }
    }
}
