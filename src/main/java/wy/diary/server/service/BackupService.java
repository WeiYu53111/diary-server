package wy.diary.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import wy.diary.server.dao.DiaryDao;
import wy.diary.server.entity.Diary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    @Autowired
    private DiaryDao diaryDao;

    @Value("${image.storage.path}")
    private String imageStoragePath;

    @Value("${backup.store.path}")
    private String backupStorePath;

    @Value("${backup.max-history:7}")
    private int maxBackupHistory;

    @Value("${backup.cron}")
    private String backupCron;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 实现 InitializingBean 接口的方法，替代 @PostConstruct
    @Override
    public void afterPropertiesSet() {
        logger.info("BackupService 初始化完成，定时备份配置为: {}", backupCron);
        logger.info("备份存储路径: {}", backupStorePath);
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
     * 执行全量备份操作 - 从数据库读取所有用户数据
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

        logger.info("正在创建全量备份文件: {}", backupFilePath);

        // 创建ZIP文件
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(backupFilePath))) {
            // 从数据库获取所有用户数据并按用户分组备份
            List<String> allOpenIds = diaryDao.getAllDistinctOpenIds();
            logger.info("共发现 {} 个用户需要备份", allOpenIds.size());

            for (String openId : allOpenIds) {
                backupUserDataToZip(zipOut, openId);
            }

            // 备份图片文件
            File imageDir = new File(imageStoragePath);
            if (imageDir.exists()) {
                addFolderToZip(imageDir, "images/", zipOut);
            }
        }

        logger.info("全量备份文件创建成功: {}", backupFilePath);
    }

    /**
     * 将用户数据备份到ZIP文件中（按用户和年份分文件）
     */
    private void backupUserDataToZip(ZipOutputStream zipOut, String openId) throws IOException {
        try {
            // 获取用户所有日记数据，按年份分组
            Map<String, List<Diary>> diariesByYear = getDiariesGroupedByYear(openId);
            
            if (diariesByYear.isEmpty()) {
                logger.debug("用户 {} 没有日记数据", openId);
                return;
            }

            // 为每个年份创建一个JSON文件
            for (Map.Entry<String, List<Diary>> entry : diariesByYear.entrySet()) {
                String year = entry.getKey();
                List<Diary> diaries = entry.getValue();
                
                // 创建JSON文件名，格式：{openid}-年份.json
                String fileName = openId + "-" + year + ".json";
                
                // 将日记转换为原有JSON格式
                JSONObject yearData = convertDiariesToJsonFormat(diaries);
                
                // 添加JSON文件到ZIP
                ZipEntry entry1 = new ZipEntry("diary/" + fileName);
                zipOut.putNextEntry(entry1);
                zipOut.write(yearData.toString(2).getBytes("UTF-8"));
                zipOut.closeEntry();
                
                logger.debug("已备份用户 {} 的 {} 年数据，共 {} 条日记", openId, year, diaries.size());
            }
        } catch (Exception e) {
            logger.error("备份用户 {} 数据失败", openId, e);
        }
    }

    /**
     * 获取用户日记数据，按年份分组
     */
    private Map<String, List<Diary>> getDiariesGroupedByYear(String openId) {
        Map<String, List<Diary>> groupedDiaries = new HashMap<>();
        
        try {
            // 获取用户所有日记（不分页）
            List<Diary> allDiaries = diaryDao.getDiariesByOpenId(openId);
            
            // 按年份分组
            for (Diary diary : allDiaries) {
                String logTime = diary.getLogTime();
                if (logTime != null && logTime.length() >= 4) {
                    String year = logTime.substring(0, 4);
                    groupedDiaries.computeIfAbsent(year, k -> new ArrayList<>()).add(diary);
                }
            }
        } catch (Exception e) {
            logger.error("获取用户 {} 的日记数据失败", openId, e);
        }
        
        return groupedDiaries;
    }

    /**
     * 将数据库日记数据转换为原有的JSON格式
     */
    private JSONObject convertDiariesToJsonFormat(List<Diary> diaries) {
        JSONObject jsonData = new JSONObject();
        
        // 用于生成key的计数器，按日期分组
        Map<String, Integer> dateCounters = new HashMap<>();
        
        for (Diary diary : diaries) {
            try {
                String logTime = diary.getLogTime(); // 格式：YYYY-MM-DD
                if (logTime == null) continue;
                
                // 生成key，格式：YYYY-MM-DDXX（XX是同一天的序号）
                String dateKey = logTime.replace("-", "");
                int sequence = dateCounters.getOrDefault(dateKey, 0) + 1;
                dateCounters.put(dateKey, sequence);
                String key = dateKey + String.format("%02d", sequence);
                
                // 创建日记JSON对象
                JSONObject diaryJson = new JSONObject();
                diaryJson.put("diaryId", diary.getDiaryId());
                diaryJson.put("editorContent", diary.getEditorContent() != null ? diary.getEditorContent() : "");
                diaryJson.put("createTime", diary.getCreateTime() != null ? diary.getCreateTime() : "");
                diaryJson.put("logTime", diary.getLogTime() != null ? diary.getLogTime() : "");
                diaryJson.put("logWeek", diary.getLogWeek() != null ? diary.getLogWeek() : "");
                diaryJson.put("logLunar", diary.getLogLunar() != null ? diary.getLogLunar() : "");
                diaryJson.put("address", diary.getAddress() != null ? diary.getAddress() : "");
                
                // 处理图片URL数组
                List<String> imageUrls = parseImageUrls(diary.getImageUrls());
                diaryJson.put("imageUrls", imageUrls);
                
                jsonData.put(key, diaryJson);
            } catch (Exception e) {
                logger.error("转换日记数据失败，diaryId: {}", diary.getDiaryId(), e);
            }
        }
        
        return jsonData;
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
     * 添加目录到ZIP文件
     */
    private void addFolderToZip(File folder, String parentPath, ZipOutputStream zipOut) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        
        for (File file : files) {
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
                zipOut.closeEntry();
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
        logger.info("旧备份文件清理完成,剩余备份文件数量: {}", Math.min(backupFiles.length, maxBackupHistory));
    }

    /**
     * 为指定用户创建备份 - 从数据库读取数据
     * 
     * @param userId 用户ID（openId）
     * @return 创建的备份文件
     * @throws IOException 如果备份过程中出现IO异常
     */
    public File createUserBackup(String userId) throws IOException {
        logger.info("开始为用户 {} 创建数据备份", userId);
        
        // 创建临时ZIP文件
        File tempFile = File.createTempFile("user-backup-"+userId, ".zip");
        
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempFile))) {
            // 备份用户日记数据
            backupUserDataToZip(zipOut, userId);
            
            // 用户图片目录
            File userImagesDir = new File(imageStoragePath, userId);
            
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