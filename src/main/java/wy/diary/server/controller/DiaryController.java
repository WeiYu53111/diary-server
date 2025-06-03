package wy.diary.server.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import wy.diary.server.dto.DiarySaveDTO;
import wy.diary.server.dto.request.DeleteDiaryRequest;
import wy.diary.server.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {
    private static final Logger logger = LoggerFactory.getLogger(DiaryController.class);

    @Value("${diary.storage.path:diary}")
    private String storagePath;

    @PostMapping("/save")
    public Map<String, Object> saveDiary(@RequestBody DiarySaveDTO diaryDto) {

        //增加日志
        System.out.println("DiaryController.saveDiary() called with: diaryDto = [" + diaryDto + "]");

        try {
            // 确保目录存在
            Path dirPath = Paths.get(storagePath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 获取当前年份
            String year = new SimpleDateFormat("yyyy").format(new Date());
            String filename = diaryDto.getOpenId() + "-" + year + ".json";
            Path filePath = dirPath.resolve(filename);

            // 读取现有文件或创建新文件
            JSONObject json;
            if (Files.exists(filePath)) {
                String content = new String(Files.readAllBytes(filePath));
                json = new JSONObject(content);
            } else {
                json = new JSONObject();
            }

            // 生成key: 日期+序号
            String dateKey = diaryDto.getLogTime();
            int seq = 1;
            String fullKey;
            do {
                fullKey = dateKey + String.format("%02d", seq++);
            } while (json.has(fullKey));

            // 保存数据
            JSONObject diaryJson = new JSONObject();
            diaryJson.put("editorContent", diaryDto.getEditorContent());
            diaryJson.put("createTime", diaryDto.getCreateTime());
            diaryJson.put("logTime", diaryDto.getLogTime());
            diaryJson.put("logWeek", diaryDto.getLogWeek());
            diaryJson.put("logLunar", diaryDto.getLogLunar());
            diaryJson.put("address", diaryDto.getAddress());
            diaryJson.put("diaryId", diaryDto.getDiaryId());
            diaryJson.put("imageUrls", diaryDto.getImageUrls());
            
            json.put(fullKey, diaryJson);

            // 写入文件
            Files.write(filePath, json.toString().getBytes());

            Map<String, String> data = new HashMap<>();
            data.put("key", fullKey);
            
            return ApiResponse.success("日记保存成功", data);
        } catch (Exception e) {
            return ApiResponse.error("保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/getDiaryId")
    public Map<String, Object> getDiaryId() {
        // 生成唯一ID
        String diaryId = UUID.randomUUID().toString();
        
        Map<String, String> data = new HashMap<>();
        data.put("diaryId", diaryId);
        
        return ApiResponse.success("ID生成成功", data);
    }

    /**
     * 获取日记列表（分页）
     * @param pageIndex 页码，从1开始
     * @param pageSize 每页记录数
     * @param openid 用户ID (由拦截器注入)
     * @return 分页的日记列表
     */
    @GetMapping("/list")
    public Map<String, Object> listDiaries(
            @RequestParam(defaultValue = "1") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestAttribute("openid") String openid) {
        
        try {
            // 获取目录下该用户的所有日记文件
            Path dirPath = Paths.get(storagePath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            
            List<Path> userFiles = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, openid + "-*.json")) {
                for (Path path : stream) {
                    userFiles.add(path);
                }
            }
            
            List<Map<String, Object>> allDiaries = new ArrayList<>();
            
            // 从每个文件中读取日记数据
            for (Path filePath : userFiles) {
                if (!Files.exists(filePath)) {
                    continue;
                }
                
                // 读取文件内容
                String content = new String(Files.readAllBytes(filePath));
                JSONObject json = new JSONObject(content);
                
                // 将JSON对象转换为列表
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject diary = json.getJSONObject(key);
                    
                    Map<String, Object> diaryMap = new HashMap<>();
                    diaryMap.put("key", key);
                    diaryMap.put("diaryId", diary.optString("diaryId", ""));
                    diaryMap.put("editorContent", diary.optString("editorContent", ""));
                    diaryMap.put("createTime", diary.optString("createTime", ""));
                    diaryMap.put("logTime", diary.optString("logTime", ""));
                    diaryMap.put("logWeek", diary.optString("logWeek", ""));
                    diaryMap.put("logLunar", diary.optString("logLunar", ""));
                    diaryMap.put("address", diary.optString("address", ""));
                    
                    // 处理图片URL数组
                    JSONArray imageUrlsArray = diary.optJSONArray("imageUrls");
                    List<String> imageUrls = new ArrayList<>();
                    if (imageUrlsArray != null) {
                        for (int i = 0; i < imageUrlsArray.length(); i++) {
                            String fullPath = imageUrlsArray.getString(i);
                            // 从路径中提取文件名和后缀
                            String fileName = Paths.get(fullPath).getFileName().toString();
                            imageUrls.add(fileName);
                        }
                    }
                    diaryMap.put("imageUrls", imageUrls);
                    allDiaries.add(diaryMap);
                }
            }
            
            // 如果没有找到任何日记，返回空列表
            if (allDiaries.isEmpty()) {
                Map<String, Object> paginationData = createPaginationData(new ArrayList<>(), pageIndex, pageSize, 0);
                return ApiResponse.success("获取日记列表成功", paginationData);
            }
            
            // 按日期降序排序（最新的日记在前面）
            allDiaries.sort((d1, d2) -> {
                String logTime1 = (String) d1.get("logTime");
                String logTime2 = (String) d2.get("logTime");
                return logTime2.compareTo(logTime1); // 降序
            });
            
            // 计算总记录数
            int totalCount = allDiaries.size();
            
            // 分页处理
            int startIndex = (pageIndex - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalCount);
            
            List<Map<String, Object>> diaries = new ArrayList<>();
            if (startIndex < totalCount) {
                diaries = allDiaries.subList(startIndex, endIndex);
            }
            
            // 构建分页响应数据
            Map<String, Object> paginationData = createPaginationData(diaries, pageIndex, pageSize, totalCount);
            
            return ApiResponse.success("获取日记列表成功", paginationData);
            
        } catch (Exception e) {
            return ApiResponse.error("获取日记列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除日记
     *
     * @param openid 用户ID (由拦截器注入)
     * @return 删除结果
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteDiary(
            @RequestBody DeleteDiaryRequest request,
            @RequestAttribute("openid") String openid) {
        
        String diaryId = request.getDiaryId();
        String createYear = request.getCreateYear();
        
        try {
            if (diaryId == null || diaryId.isEmpty()) {
                return ApiResponse.error("日记ID不能为空");
            }
            
            // 获取目录下该用户的所有日记文件
            Path dirPath = Paths.get(storagePath);
            if (!Files.exists(dirPath)) {
                return ApiResponse.error("日记目录不存在");
            }
            
            List<Path> userFiles = new ArrayList<>();
            
            // 如果提供了创建年份，直接定位到对应年份的文件
            if (createYear != null && !createYear.isEmpty()) {
                Path targetFilePath = dirPath.resolve(openid + "-" + createYear + ".json");
                if (Files.exists(targetFilePath)) {
                    userFiles.add(targetFilePath);
                } else {
                    return ApiResponse.error("未找到" + createYear + "年的日记文件");
                }
            } else {
                // 否则遍历所有文件
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, openid + "-*.json")) {
                    for (Path path : stream) {
                        userFiles.add(path);
                    }
                }
            }
            
            if (userFiles.isEmpty()) {
                return ApiResponse.error("未找到用户日记文件");
            }
            
            boolean diaryFound = false;
            String deletedKey = null;
            Path modifiedFilePath = null;
            int imagesDeleted = 0; // 记录删除的图片数量
            
            // 从每个文件中查找并删除日记
            for (Path filePath : userFiles) {
                if (!Files.exists(filePath)) {
                    continue;
                }
                
                // 读取文件内容
                String content = new String(Files.readAllBytes(filePath));
                JSONObject json = new JSONObject(content);
                
                // 查找包含指定diaryId的日记
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject diary = json.getJSONObject(key);
                    
                    String currentDiaryId = diary.optString("diaryId", "");
                    if (diaryId.equals(currentDiaryId)) {
                        // 找到匹配的日记，删除相关的图片文件
                        JSONArray imageUrlsArray = diary.optJSONArray("imageUrls");
                        if (imageUrlsArray != null && !imageUrlsArray.isEmpty()) {
                            for (int i = 0; i < imageUrlsArray.length(); i++) {
                                String imagePath = imageUrlsArray.getString(i);
                                try {
                                    // 删除图片文件
                                    Path imageFilePath = Paths.get(imagePath);
                                    if (Files.exists(imageFilePath)) {
                                        Files.delete(imageFilePath);
                                        logger.info("已删除日记图片: {}", imagePath);
                                        imagesDeleted++; // 增加删除计数
                                    } else {
                                        logger.warn("日记图片不存在，无法删除: {}", imagePath);
                                    }
                                } catch (Exception e) {
                                    logger.error("删除日记图片失败: {}, 错误: {}", imagePath, e.getMessage(), e);
                                }
                            }
                        }
                        
                        // 移除日记条目
                        keys.remove();
                        diaryFound = true;
                        deletedKey = key;
                        modifiedFilePath = filePath;
                        break;
                    }
                }
                
                // 如果找到并删除了日记，更新文件
                if (diaryFound) {
                    Files.write(filePath, json.toString().getBytes());
                    break;
                }
            }
            
            if (!diaryFound) {
                return ApiResponse.error("未找到指定ID的日记");
            }
            
//            Map<String, Object> data = new HashMap<>();
//            data.put("diaryId", diaryId);
//            data.put("key", deletedKey);
//            data.put("file", modifiedFilePath.getFileName().toString());
//            data.put("imagesDeleted", imagesDeleted); // 添加删除的图片数量信息
            return ApiResponse.success("日记删除成功", true);
            
        } catch (Exception e) {
            return ApiResponse.error("删除日记失败: " + e.getMessage());
        }
    }

    /**
     * 创建分页数据结构
     * @param records 当前页的记录
     * @param pageIndex 当前页码
     * @param pageSize 每页大小
     * @param totalCount 总记录数
     * @return 包含分页信息的数据结构
     */
    private Map<String, Object> createPaginationData(List<?> records, int pageIndex, int pageSize, int totalCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("pageIndex", pageIndex);
        data.put("pageSize", pageSize);
        data.put("totalCount", totalCount);
        data.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));
        data.put("hasNext", pageIndex * pageSize < totalCount);
        data.put("hasPrevious", pageIndex > 1);
        return data;
    }
}
