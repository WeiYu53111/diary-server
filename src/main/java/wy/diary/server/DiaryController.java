package wy.diary.server;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import wy.diary.server.dto.DiarySaveDTO;
import wy.diary.server.model.ApiResponse;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    @Value("${diary.storage.path:diary}")
    private String storagePath;

    @PostMapping("/save")
    public Map<String, Object> saveDiary(@RequestBody DiarySaveDTO diaryDto) {
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
}
