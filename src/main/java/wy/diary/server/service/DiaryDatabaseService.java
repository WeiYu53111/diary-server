package wy.diary.server.service;

import wy.diary.server.dao.DiaryDao;
import wy.diary.server.dto.DiarySaveDTO;
import wy.diary.server.entity.Diary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 日记数据库服务类
 */
@Service
public class DiaryDatabaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(DiaryDatabaseService.class);
    
    @Autowired
    private DiaryDao diaryDao;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 保存日记
     */
    @Transactional
    public String saveDiary(DiarySaveDTO diaryDto) {
        try {
            Diary diary = new Diary();
            diary.setDiaryId(diaryDto.getDiaryId());
            diary.setOpenId(diaryDto.getOpenId());
            diary.setEditorContent(diaryDto.getEditorContent());
            diary.setCreateTime(diaryDto.getCreateTime());
            diary.setLogTime(diaryDto.getLogTime());
            diary.setLogWeek(diaryDto.getLogWeek());
            diary.setLogLunar(diaryDto.getLogLunar());
            diary.setAddress(diaryDto.getAddress());
            
            // 将图片URL列表转换为JSON字符串存储
            if (diaryDto.getImageUrls() != null && diaryDto.getImageUrls().length > 0) {
                diary.setImageUrls(objectMapper.writeValueAsString(diaryDto.getImageUrls()));
            } else {
                diary.setImageUrls("[]");
            }
            
            int result = diaryDao.saveDiary(diary);
            if (result > 0) {
                logger.info("日记保存成功，diaryId: {}", diary.getDiaryId());
                return diary.getDiaryId();
            } else {
                throw new RuntimeException("日记保存失败");
            }
        } catch (Exception e) {
            logger.error("保存日记失败", e);
            throw new RuntimeException("保存日记失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据用户OpenID分页查询日记列表
     */
    public Map<String, Object> getDiariesByOpenId(String openId, int pageIndex, int pageSize) {
        try {
            List<Diary> diaries = diaryDao.getDiariesByOpenId(openId, pageIndex, pageSize);
            int totalCount = diaryDao.getDiaryCountByOpenId(openId);
            
            List<Map<String, Object>> diaryList = new ArrayList<>();
            for (Diary diary : diaries) {
                Map<String, Object> diaryMap = convertDiaryToMap(diary);
                diaryList.add(diaryMap);
            }
            
            // 构建分页响应数据
            Map<String, Object> paginationData = createPaginationData(diaryList, pageIndex, pageSize, totalCount);
            
            return paginationData;
        } catch (Exception e) {
            logger.error("查询日记列表失败", e);
            throw new RuntimeException("查询日记列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除日记
     */
    @Transactional
    public boolean deleteDiary(String diaryId) {
        try {
            // 先查询日记是否存在
            Diary diary = diaryDao.getDiaryByDiaryId(diaryId);
            if (diary == null) {
                logger.warn("要删除的日记不存在，diaryId: {}", diaryId);
                return false;
            }
            
            // 删除相关的图片文件
            deleteImageFiles(diary.getImageUrls());
            
            // 逻辑删除日记
            int result = diaryDao.deleteDiary(diaryId);
            if (result > 0) {
                logger.info("日记删除成功，diaryId: {}", diaryId);
                return true;
            } else {
                logger.warn("日记删除失败，diaryId: {}", diaryId);
                return false;
            }
        } catch (Exception e) {
            logger.error("删除日记失败", e);
            throw new RuntimeException("删除日记失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据用户OpenID查询所有日记列表（不分页）
     */
    public List<Diary> getDiariesByOpenId(String openId) {
        try {
            // 查询大量数据，设置较大的pageSize来获取所有记录
            return diaryDao.getDiariesByOpenId(openId, 1, 10000);
        } catch (Exception e) {
            logger.error("查询用户所有日记失败", e);
            throw new RuntimeException("查询日记列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询所有日记列表（不分页）
     */
    public List<Diary> getAllDiaries() {
        try {
            return diaryDao.getAllDiaries();
        } catch (Exception e) {
            logger.error("查询所有日记失败", e);
            throw new RuntimeException("查询日记列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户OpenID和年份查询日记列表
     */
    public List<Diary> getDiariesByOpenIdAndYear(String openId, String year) {
        try {
            return diaryDao.getDiariesByOpenIdAndYear(openId, year);
        } catch (Exception e) {
            logger.error("根据年份查询日记列表失败", e);
            throw new RuntimeException("查询日记列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 将Diary实体转换为Map
     */
    private Map<String, Object> convertDiaryToMap(Diary diary) {
        Map<String, Object> diaryMap = new HashMap<>();
        diaryMap.put("key", generateKey(diary.getLogTime(), diary.getId()));
        diaryMap.put("diaryId", diary.getDiaryId());
        diaryMap.put("editorContent", diary.getEditorContent());
        diaryMap.put("createTime", diary.getCreateTime());
        diaryMap.put("logTime", diary.getLogTime());
        diaryMap.put("logWeek", diary.getLogWeek());
        diaryMap.put("logLunar", diary.getLogLunar());
        diaryMap.put("address", diary.getAddress());
        
        // 处理图片URL数组
        List<String> imageUrls = parseImageUrls(diary.getImageUrls());
        List<String> fileNames = new ArrayList<>();
        for (String fullPath : imageUrls) {
            // 从路径中提取文件名和后缀
            String fileName = java.nio.file.Paths.get(fullPath).getFileName().toString();
            fileNames.add(fileName);
        }
        diaryMap.put("imageUrls", fileNames);
        
        return diaryMap;
    }
    
    /**
     * 生成Key（兼容原有格式）
     */
    private String generateKey(String logTime, Long id) {
        // 使用logTime + ID的方式生成唯一key
        return logTime + String.format("%02d", id % 100);
    }
    
    /**
     * 解析图片URL JSON字符串
     */
    private List<String> parseImageUrls(String imageUrlsJson) {
        try {
            if (imageUrlsJson == null || imageUrlsJson.trim().isEmpty() || "[]".equals(imageUrlsJson.trim())) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(imageUrlsJson, List.class);
        } catch (Exception e) {
            logger.error("解析图片URL失败: {}", imageUrlsJson, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 删除图片文件
     */
    private void deleteImageFiles(String imageUrlsJson) {
        try {
            List<String> imageUrls = parseImageUrls(imageUrlsJson);
            int imagesDeleted = 0;
            for (String imagePath : imageUrls) {
                try {
                    java.nio.file.Path imageFilePath = java.nio.file.Paths.get(imagePath);
                    if (java.nio.file.Files.exists(imageFilePath)) {
                        java.nio.file.Files.delete(imageFilePath);
                        logger.info("已删除日记图片: {}", imagePath);
                        imagesDeleted++;
                    } else {
                        logger.warn("日记图片不存在，无法删除: {}", imagePath);
                    }
                } catch (Exception e) {
                    logger.error("删除日记图片失败: {}, 错误: {}", imagePath, e.getMessage(), e);
                }
            }
            logger.info("删除图片文件完成，共删除 {} 个文件", imagesDeleted);
        } catch (Exception e) {
            logger.error("删除图片文件失败", e);
        }
    }
    
    /**
     * 创建分页数据结构
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