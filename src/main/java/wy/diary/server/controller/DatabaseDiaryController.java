package wy.diary.server.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import wy.diary.server.dto.DiarySaveDTO;
import wy.diary.server.dto.request.DeleteDiaryRequest;
import wy.diary.server.model.ApiResponse;
import wy.diary.server.service.DiaryDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 基于数据库的日记控制器
 */
@RestController
@RequestMapping("/api/db/diary")
public class DatabaseDiaryController {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseDiaryController.class);

    @Autowired
    private DiaryDatabaseService diaryDatabaseService;

    @PostMapping("/save")
    public Map<String, Object> saveDiary(@RequestBody DiarySaveDTO diaryDto) {
        //增加日志
        logger.info("DatabaseDiaryController.saveDiary() called with: diaryDto = [{}]", diaryDto);

        try {
            String diaryId = diaryDatabaseService.saveDiary(diaryDto);

            Map<String, String> data = new HashMap<>();
            data.put("diaryId", diaryId);
            
            return ApiResponse.success("日记保存成功", data);
        } catch (Exception e) {
            logger.error("保存日记失败", e);
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
            Map<String, Object> paginationData = diaryDatabaseService.getDiariesByOpenId(openid, pageIndex, pageSize);
            return ApiResponse.success("获取日记列表成功", paginationData);
            
        } catch (Exception e) {
            logger.error("获取日记列表失败", e);
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
        
        try {
            if (diaryId == null || diaryId.isEmpty()) {
                return ApiResponse.error("日记ID不能为空");
            }
            
            boolean success = diaryDatabaseService.deleteDiary(diaryId);
            
            if (success) {
                return ApiResponse.success("日记删除成功", true);
            } else {
                return ApiResponse.error("未找到指定ID的日记");
            }
            
        } catch (Exception e) {
            logger.error("删除日记失败", e);
            return ApiResponse.error("删除日记失败: " + e.getMessage());
        }
    }
} 