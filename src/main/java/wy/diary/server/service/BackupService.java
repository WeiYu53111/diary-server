package wy.diary.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    @Value("${diary.storage.path}")
    private String diaryStoragePath;

    @Value("${image.storage.path}")
    private String imageStoragePath;

    @Value("${backup.store.path}")
    private String backupStorePath;

    @Value("${backup.max-history:7}")
    private int maxBackupHistory;

    @Value("${backup.cron}")
    private String backupCron;

    @Value("${diary.data.directory:/data/diary}")
    private String diaryDataDirectory;

    @Value("${diary.images.directory:/data/images}")
    private String diaryImagesDirectory;

    // 实现 InitializingBean 接口的方法，替代 @PostConstruct
    @Override
    public void afterPropertiesSet() {
        logger.info("BackupService 初始化完成，定时备份配置为: {}", backupCron);
        logger.info("备份存储路径: {}", backupStorePath);
        logger.info("日记存储路径: {}", diaryStoragePath);
        logger.info("图片存储路径: {}", imageStoragePath);
    }

    /**
     * 定时备份任务，按照配置的cron表达式执行
     */
    @Scheduled(cron = "${backup.cron}")
    public void scheduledBackup() {
        logger.info("开始执行定时备份任务");
        try {
            createBackup();
            cleanOldBackups();
        } catch (Exception e) {
            logger.error("定时备份任务执行失败", e);
        }
    }

    /**
     * 执行备份操作
     */
    public void createBackup() throws IOException {
        // 确保备份目录存在
        File backupDir = new File(backupStorePath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        // 创建备份文件名，格式为 fish-diary-yyyyMMdd-HHmmss.zip
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String backupFileName = "fish-diary-" + dateFormat.format(new Date()) + ".zip";
        String backupFilePath = backupStorePath + backupFileName;

        logger.info("正在创建备份文件: {}", backupFilePath);

        // 创建ZIP文件
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(backupFilePath))) {
            // 备份日记文件
            File diaryDir = new File(diaryStoragePath);
            if (diaryDir.exists()) {
                addFolderToZip(diaryDir, "diary/", zipOut);
            }

            // 备份图片文件
            File imageDir = new File(imageStoragePath);
            if (imageDir.exists()) {
                addFolderToZip(imageDir, "images/", zipOut);
            }
        }

        logger.info("备份文件创建成功: {}", backupFilePath);
    }

    /**
     * 添加目录到ZIP文件
     */
    private void addFolderToZip(File folder, String parentPath, ZipOutputStream zipOut) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                addFolderToZip(file, parentPath + file.getName() + "/", zipOut);
                continue;
            }
            
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(parentPath + file.getName());
                zipOut.putNextEntry(zipEntry);
                
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
            }
        }
    }

    /**
     * 清理旧的备份文件，只保留最近的几个
     */
    private void cleanOldBackups() throws IOException {
        logger.info("清理旧备份文件，保留最近{}个备份", maxBackupHistory);
        
        File backupDir = new File(backupStorePath);
        if (!backupDir.exists()) {
            return;
        }
        
        // 获取所有备份文件并按修改时间排序
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("fish-diary-") && name.endsWith(".zip"));
        if (backupFiles == null || backupFiles.length <= maxBackupHistory) {
            return;
        }

        // 按最后修改时间排序（最新的排在前面）
        java.util.Arrays.sort(backupFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        
        // 删除超过保留数量的旧备份
        for (int i = maxBackupHistory; i < backupFiles.length; i++) {
            if (backupFiles[i].delete()) {
                logger.info("已删除旧备份文件: {}", backupFiles[i].getName());
            } else {
                logger.warn("无法删除旧备份文件: {}", backupFiles[i].getName());
            }
        }
        logger.info("旧备份文件清理完成,剩余备份文件数量: {}", backupFiles.length - maxBackupHistory);
        logger.info("删除的备份文件名: {}", backupFiles[backupFiles.length - 1].getName());
    }

    /**
     * 为指定用户创建备份，压缩包含其日记和图片
     * 
     * @param userId 用户ID
     * @return 创建的备份文件
     * @throws IOException 如果备份过程中出现IO异常
     */
    public File createUserBackup(String userId) throws IOException {
        logger.info("开始为用户 {} 创建数据备份", userId);
        
        // 创建临时ZIP文件
        File tempFile = File.createTempFile("user-backup-"+userId, ".zip");
        
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempFile))) {
            // 基于文件名模式查找用户日记文件
            File diaryDataDir = new File(diaryDataDirectory);
            // 检查目录是否存在
            if (!diaryDataDir.exists() || !diaryDataDir.isDirectory()) {
                logger.warn("日记数据目录不存在: {}", diaryDataDirectory);
            } else {
                // 查找用户的所有日记文件（格式：userId+年份.json）
                File[] userDiaryFiles = diaryDataDir.listFiles((dir, name) -> 
                    name.startsWith(userId) && name.endsWith(".json"));
                
                if (userDiaryFiles != null && userDiaryFiles.length > 0) {
                    logger.info("找到用户 {} 的日记文件 {} 个", userId, userDiaryFiles.length);
                    
                    // 添加日记文件到ZIP
                    for (File diaryFile : userDiaryFiles) {
                        addFileToZip(zipOut, diaryFile, "diary/" + diaryFile.getName());
                        logger.debug("已添加日记文件到备份: {}", diaryFile.getName());
                    }
                } else {
                    logger.warn("未找到用户 {} 的日记文件", userId);
                }
            }
            
            // 用户图片目录
            File userImagesDir = new File(diaryImagesDirectory, userId);
            
            // 检查并备份图片目录
            if (userImagesDir.exists() && userImagesDir.isDirectory()) {
                logger.info("正在备份用户图片目录: {}", userImagesDir.getAbsolutePath());
                addDirectoryToZip(zipOut, userImagesDir, userId + "/images/");
            } else {
                logger.warn("用户图片目录不存在: {}", userImagesDir.getAbsolutePath());
            }
        }
        
        logger.info("用户 {} 的备份创建完成，文件大小: {} 字节", userId, tempFile.length());
        return tempFile;
    }

    /**
     * 添加单个文件到ZIP
     * 
     * @param zipOut ZIP输出流
     * @param file 要添加的文件
     * @param entryPath ZIP内的路径
     * @throws IOException 如果添加过程中出现IO异常
     */
    private void addFileToZip(ZipOutputStream zipOut, File file, String entryPath) throws IOException {
        ZipEntry entry = new ZipEntry(entryPath);
        zipOut.putNextEntry(entry);
        
        byte[] buffer = new byte[8192]; // 8KB buffer
        try (FileInputStream fis = new FileInputStream(file)) {
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zipOut.write(buffer, 0, length);
            }
        }
        
        zipOut.closeEntry();
    }
    
    /**
     * 递归地将目录及其内容添加到ZIP文件中
     * 
     * @param zipOut ZIP输出流
     * @param directory 要添加的目录
     * @param basePath ZIP内的基础路径
     * @throws IOException 如果添加过程中出现IO异常
     */
    private void addDirectoryToZip(ZipOutputStream zipOut, File directory, String basePath) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        byte[] buffer = new byte[8192]; // 8KB buffer
        
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归处理子目录
                addDirectoryToZip(zipOut, file, basePath + file.getName() + "/");
            } else {
                // 将文件添加到ZIP
                String entryPath = basePath + file.getName();
                ZipEntry entry = new ZipEntry(entryPath);
                zipOut.putNextEntry(entry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, length);
                    }
                }
                
                zipOut.closeEntry();
                logger.debug("已添加文件到备份: {}", entryPath);
            }
        }
    }
}