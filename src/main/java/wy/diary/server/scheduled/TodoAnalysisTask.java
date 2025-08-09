package wy.diary.server.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wy.diary.server.entity.Diary;
import wy.diary.server.entity.TodoItem;
import wy.diary.server.service.DiaryDatabaseService;
import wy.diary.server.service.TodoAnalysisService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 待做事项分析定时任务
 * 每日定时分析用户日记并生成待做事项
 */
@Component
public class TodoAnalysisTask {

    private static final Logger logger = LoggerFactory.getLogger(TodoAnalysisTask.class);

    @Autowired
    private TodoAnalysisService todoAnalysisService;

    @Autowired
    private DiaryDatabaseService diaryDatabaseService;

    @Value("${todo.analysis.enabled:true}")
    private boolean todoAnalysisEnabled;

    @Value("${todo.analysis.cron:0 0 1 * * ?}")
    private String todoAnalysisCron;

    /**
     * 每日凌晨1点执行待做事项分析任务
     * 可通过配置 todo.analysis.cron 自定义执行时间
     */
    @Scheduled(cron = "${todo.analysis.cron:0 0 1 * * ?}")
    public void analyzeDiariesAndGenerateTodos() {
        if (!todoAnalysisEnabled) {
            logger.info("待做事项分析功能已禁用");
            return;
        }

        logger.info("开始执行每日待做事项分析任务...");
        
        try {
            LocalDate analysisDate = LocalDate.now();
            
            // 1. 获取所有有日记的用户
            Set<String> activeUsers = getActiveUsers();
            if (activeUsers.isEmpty()) {
                logger.info("没有找到有日记的活跃用户，跳过分析");
                return;
            }

            logger.info("找到 {} 个活跃用户，开始分析", activeUsers.size());

            // 2. 为每个用户分析日记并生成待做事项
            int successCount = 0;
            int totalTodoCount = 0;

            for (String openId : activeUsers) {
                try {
                    List<TodoItem> todoItems = todoAnalysisService.analyzeAndGenerateTodos(openId, analysisDate);
                    if (!todoItems.isEmpty()) {
                        successCount++;
                        totalTodoCount += todoItems.size();
                        logger.info("为用户 {} 成功生成 {} 个待做事项", openId, todoItems.size());
                    } else {
                        logger.info("用户 {} 未生成待做事项", openId);
                    }
                } catch (Exception e) {
                    logger.error("为用户 {} 分析日记时发生错误", openId, e);
                }
            }

            logger.info("每日待做事项分析任务完成。处理用户数: {}/{}, 总生成待做事项数: {}", 
                       successCount, activeUsers.size(), totalTodoCount);

        } catch (Exception e) {
            logger.error("执行每日待做事项分析任务时发生错误", e);
        }
    }

    /**
     * 获取有活跃日记的用户列表
     * 定义为最近30天内有日记记录的用户
     */
    private Set<String> getActiveUsers() {
        try {
            List<Diary> allDiaries = diaryDatabaseService.getAllDiaries();
            LocalDate cutoffDate = LocalDate.now().minusDays(30);

            return allDiaries.stream()
                    .filter(diary -> {
                        if (diary.getLogTime() == null || diary.getOpenId() == null) {
                            return false;
                        }
                        try {
                            LocalDate logDate = LocalDate.parse(diary.getLogTime());
                            return !logDate.isBefore(cutoffDate);
                        } catch (Exception e) {
                            logger.warn("解析日记时间失败: {}", diary.getLogTime());
                            return false;
                        }
                    })
                    .map(Diary::getOpenId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("获取活跃用户列表时发生错误", e);
            return Set.of();
        }
    }

    /**
     * 手动触发待做事项分析（用于测试或特殊情况）
     * 
     * @param openId 指定用户OpenID，如果为null则分析所有活跃用户
     */
    public void manualAnalyze(String openId) {
        logger.info("手动触发待做事项分析，用户: {}", openId != null ? openId : "所有活跃用户");
        
        try {
            LocalDate analysisDate = LocalDate.now();
            
            if (openId != null) {
                // 分析指定用户
                List<TodoItem> todoItems = todoAnalysisService.analyzeAndGenerateTodos(openId, analysisDate);
                logger.info("为用户 {} 手动分析完成，生成 {} 个待做事项", openId, todoItems.size());
            } else {
                // 分析所有活跃用户
                analyzeDiariesAndGenerateTodos();
            }
        } catch (Exception e) {
            logger.error("手动分析待做事项时发生错误", e);
        }
    }
} 