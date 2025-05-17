package wy.diary.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class BackupQueueService implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(BackupQueueService.class);
    
    /**
     * 备份任务状态枚举
     */
    public enum BackupTaskStatus {
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        EMPTY("空"),
        FAILED("失败");
        
        private final String description;
        
        BackupTaskStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return name();
        }
        
        /**
         * 创建失败状态并附加失败原因
         * @param errorMessage 失败原因
         * @return 包含失败原因的状态字符串
         */
        public static String failedWithReason(String errorMessage) {
            return FAILED + ": " + errorMessage;
        }
    }
    
    @Autowired
    private BackupService backupService;
    
    @Value("${backup.temp.directory:${java.io.tmpdir}/fish-diary-backups}")
    private String tempBackupDirectory;
    
    // 备份任务队列
    private final BlockingQueue<BackupTask> backupQueue = new LinkedBlockingQueue<>();
    
    // 备份工作线程数量，默认为1表示单线程处理备份任务
    @Value("${backup.worker.threads:1}")
    private int backupWorkerThreads;
    
    // 线程池用于执行备份任务
    private ThreadPoolExecutor executor;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化线程池，设置核心线程数为配置的backupWorkerThreads
        executor = new ThreadPoolExecutor(
                backupWorkerThreads,
                backupWorkerThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );
        
        // 启动消费者线程处理队列中的任务
        for (int i = 0; i < backupWorkerThreads; i++) {
            executor.execute(this::processBackupQueue);
        }
        
        logger.info("备份队列服务已启动，工作线程数: {}", backupWorkerThreads);
    }
    
    /**
     * 提交备份任务到队列
     */
    public void submitBackupTask(String openid, String taskId, 
                                ConcurrentMap<String, String> backupTaskStatuses,
                                ConcurrentMap<String, String> backupTaskFiles) {
        BackupTask task = new BackupTask(openid, taskId, backupTaskStatuses, backupTaskFiles);
        backupQueue.offer(task);
        logger.info("备份任务已提交到队列: {}", taskId);
    }
    
    /**
     * 处理队列中的备份任务
     */
    private void processBackupQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            BackupTask task = null;
            try {
                // 从队列中获取任务，如果没有任务则阻塞
                task = backupQueue.take();
                logger.info("开始处理备份任务: {}", task.getTaskId());
                
                // 执行备份任务
                executeBackupTask(task);
                
                logger.info("备份任务处理完成: {}", task.getTaskId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("备份任务处理线程被中断", e);
                break;
            } catch (Exception e) {
                if (task != null) {
                    task.getBackupTaskStatuses().put(task.getTaskId(), 
                        BackupTaskStatus.failedWithReason(e.getMessage()));
                    logger.error("备份任务执行失败: {}", task.getTaskId(), e);
                }
            }
        }
    }
    
    /**
     * 执行备份任务
     */
    private void executeBackupTask(BackupTask task) {
        try {
            // 确保临时目录存在
            File tempDir = new File(tempBackupDirectory);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // 备份指定用户的数据
            File backupFile = backupService.createUserBackup(task.getOpenid());
            
            // 生成临时文件名
            String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            String filename = "fish-diary-" + task.getOpenid() + "-" + date + ".zip";
            
            // 将文件移动到临时目录
            File destFile = new File(tempDir, filename);
            if (backupFile.renameTo(destFile)) {
                task.getBackupTaskFiles().put(task.getTaskId(), destFile.getAbsolutePath());
                task.getBackupTaskStatuses().put(task.getTaskId(), BackupTaskStatus.COMPLETED.toString());
            } else {
                task.getBackupTaskStatuses().put(task.getTaskId(), BackupTaskStatus.FAILED.toString());
            }
        } catch (Exception e) {
            task.getBackupTaskStatuses().put(task.getTaskId(), 
                BackupTaskStatus.failedWithReason(e.getMessage()));
        }
    }
    
    /**
     * 备份任务类
     */
    private static class BackupTask {
        private final String openid;
        private final String taskId;
        private final ConcurrentMap<String, String> backupTaskStatuses;
        private final ConcurrentMap<String, String> backupTaskFiles;
        
        public BackupTask(String openid, String taskId, 
                         ConcurrentMap<String, String> backupTaskStatuses,
                         ConcurrentMap<String, String> backupTaskFiles) {
            this.openid = openid;
            this.taskId = taskId;
            this.backupTaskStatuses = backupTaskStatuses;
            this.backupTaskFiles = backupTaskFiles;
        }
        
        public String getOpenid() {
            return openid;
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public ConcurrentMap<String, String> getBackupTaskStatuses() {
            return backupTaskStatuses;
        }
        
        public ConcurrentMap<String, String> getBackupTaskFiles() {
            return backupTaskFiles;
        }
    }
}