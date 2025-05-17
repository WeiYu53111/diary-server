package wy.diary.server.controller;

import wy.diary.server.model.ApiResponse;
import wy.diary.server.service.BackupService;
import wy.diary.server.service.BackupQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/api/backup")
public class BackupController {
    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);

    @Autowired
    private BackupService backupService;
    
    @Autowired
    private BackupQueueService backupQueueService;
    
    @Value("${backup.temp.directory:${java.io.tmpdir}/fish-diary-backups}")
    private String tempBackupDirectory;
    
    // 存储任务ID与备份状态的映射
    private final ConcurrentMap<String, String> backupTaskStatuses = new ConcurrentHashMap<>();
    // 存储任务ID与备份文件路径的映射
    private final ConcurrentMap<String, String> backupTaskFiles = new ConcurrentHashMap<>();

    @PostMapping("/trigger")
    public Map<String, Object> triggerBackup() {
        logger.info("收到全量备份触发请求");
        try {
            logger.info("开始执行全量备份");
            backupService.createBackup();
            logger.info("全量备份执行成功");
            return ApiResponse.success("备份成功", null);
        } catch (Exception e) {
            logger.error("全量备份执行失败: {}", e.getMessage(), e);
            return ApiResponse.error("备份失败: " + e.getMessage());
        }
    }

    /**
     * 异步启动用户数据备份
     * @param openid 用户ID
     * @return 任务ID，用于后续查询备份状态
     */
    @PostMapping("/user/start")
    public Map<String, Object> startUserBackup(@RequestAttribute("openid") String openid) {
        logger.info("用户 [{}] 请求启动数据备份", openid);
        
        // 检查当前时间是否在凌晨3点到4点之间
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (hour == 3) {
            logger.warn("用户 [{}] 在系统维护时间段(凌晨3点)请求备份，已拒绝", openid);
            return ApiResponse.error("系统维护中，凌晨3点到4点不接受备份请求");
        }
        
        // 检查是否有正在进行的备份任务，使用openid作为key前缀
        for (String taskId : backupTaskStatuses.keySet()) {
            if (taskId.startsWith(openid + "-")) {
                String status = backupTaskStatuses.get(taskId);
                if (status.equals(BackupQueueService.BackupTaskStatus.PROCESSING.toString())) {
                    logger.warn("用户 [{}] 已有正在进行的备份任务 [{}]，状态: {}", openid, taskId, status);
                    return ApiResponse.error("已有备份任务正在进行，请稍后再试");
                }
                break;
            }
        }

        // 生成唯一的任务ID
        String taskId = openid + "-" + new SimpleDateFormat("yyyyMMddHH").format(new Date());
        logger.info("为用户 [{}] 生成备份任务ID: {}", openid, taskId);
        
        // 更新任务状态
        backupTaskStatuses.put(taskId, BackupQueueService.BackupTaskStatus.PROCESSING.toString());
        logger.info("备份任务 [{}] 状态已设置为处理中", taskId);
        
        // 提交备份任务到队列
        logger.info("提交备份任务 [{}] 到队列", taskId);
        backupQueueService.submitBackupTask(openid, taskId, backupTaskStatuses, backupTaskFiles);
        
        Map<String, String> data = new HashMap<>();
        data.put("taskId", taskId);
        logger.info("备份任务 [{}] 已成功提交", taskId);
        return ApiResponse.success("备份任务已提交到队列", data);
    }
    
    /**
     * 查询备份任务状态
     * @param openid 用户ID
     * @return 任务状态、任务ID
     */
    @GetMapping("/status")
    public Map<String, Object> getBackupStatus(@RequestAttribute("openid") String openid) {
        logger.info("用户 [{}] 请求查询备份任务状态", openid);

        // 根据openid获取任务ID
        String taskId = "";
        // 检查是否有备份任务，使用openid作为key前缀
        for (String oldTaskId : backupTaskStatuses.keySet()) {
            if (oldTaskId.startsWith(openid + "-")) {
                taskId = oldTaskId;
                break;
            }
        }
        Map<String, String> data = new HashMap<>();
        if (taskId.isEmpty()) {
            logger.info("未找到用户 [{}] 的备份任务", openid);
            data.put("status", BackupQueueService.BackupTaskStatus.EMPTY.toString());
            return ApiResponse.success("任务状态为空", data);
        }

        String status = backupTaskStatuses.get(taskId);
        if (status == null) {
            logger.warn("用户 [{}] 的备份任务 [{}] 存在，但状态为空", openid, taskId);
            data.put("status", BackupQueueService.BackupTaskStatus.EMPTY.toString());
            return ApiResponse.success("任务状态为空", data);
        }
        
        logger.info("用户 [{}] 的备份任务 [{}] 状态为: {}", openid, taskId, status);

        data.put("status", status);
        data.put("taskId", taskId);
        return ApiResponse.success("获取状态成功", data);
    }
    
    /**
     * 下载已完成的备份文件
     * @param taskId 任务ID
     * @return 备份的ZIP文件
     */
    @GetMapping("/download/{taskId}")
    public ResponseEntity<?> downloadBackup(@PathVariable String taskId) {
        logger.info("收到下载备份文件请求，任务ID: [{}]", taskId);
        
        String status = backupTaskStatuses.get(taskId);
        String filePath = backupTaskFiles.get(taskId);
        
        if (status == null || filePath == null) {
            logger.warn("备份下载失败: 任务 [{}] 不存在或未关联文件路径", taskId);
            return ResponseEntity.badRequest().body(ApiResponse.error("备份未完成或不存在"));
        }
        
        if (!status.equals(BackupQueueService.BackupTaskStatus.COMPLETED.toString())) {
            logger.warn("备份下载失败: 任务 [{}] 状态为 [{}]，未完成", taskId, status);
            return ResponseEntity.badRequest().body(ApiResponse.error("备份未完成或不存在"));
        }
        
        try {
            File backupFile = new File(filePath);
            if (!backupFile.exists()) {
                logger.error("备份下载失败: 任务 [{}] 文件不存在，路径: {}", taskId, filePath);
                return ResponseEntity.badRequest().body(ApiResponse.error("备份文件不存在"));
            }
            
            logger.info("开始下载备份文件: 任务 [{}]，文件大小: {} 字节", taskId, backupFile.length());
            Path path = Paths.get(backupFile.getAbsolutePath());
            Resource resource = new UrlResource(path.toUri());
            
            // 下载文件时仍然需要使用特殊的ResponseEntity格式
            logger.info("备份文件 [{}] 下载已开始", taskId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backupFile.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("备份下载过程中发生错误: 任务 [{}], 错误: {}", taskId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("下载备份文件失败: " + e.getMessage()));
        }
    }

    /**
     * APP报告备份文件已下载完成，清除任务信息
     * @param taskId 任务ID
     * @return 清理结果
     */
    @GetMapping("/complete/{taskId}")
    public Map<String, Object> completeBackup(@PathVariable String taskId) {
        logger.info("收到备份下载完成通知，任务ID: [{}]", taskId);
        
        if (!backupTaskStatuses.containsKey(taskId)) {
            logger.warn("备份完成通知失败: 任务 [{}] 不存在", taskId);
            return ApiResponse.error("备份任务不存在");
        }
        
        try {
            // 获取文件路径用于删除
            String filePath = backupTaskFiles.get(taskId);
            
            // 清除任务状态和文件路径映射
            backupTaskStatuses.remove(taskId);
            backupTaskFiles.remove(taskId);
            logger.info("已清除任务 [{}] 的状态信息", taskId);
            
            // 可选: 删除备份文件以释放服务器空间
            if (filePath != null) {
                File backupFile = new File(filePath);
                if (backupFile.exists() && backupFile.delete()) {
                    logger.info("已删除任务 [{}] 的备份文件: {}", taskId, filePath);
                } else {
                    logger.warn("无法删除任务 [{}] 的备份文件: {}", taskId, filePath);
                }
            }
            
            return ApiResponse.success("备份任务已完成并清理", null);
        } catch (Exception e) {
            logger.error("清除备份任务 [{}] 信息时发生错误: {}", taskId, e.getMessage(), e);
            return ApiResponse.error("清除备份任务信息失败: " + e.getMessage());
        }
    }

}