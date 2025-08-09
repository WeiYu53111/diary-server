package wy.diary.server.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单的一次性数据迁移工具
 * 完全独立运行，不依赖Spring Boot
 * 
 * 使用方法：
 * 1. 配置数据库连接信息
 * 2. 设置JSON文件路径
 * 3. 直接运行main方法
 * 
 * mvn exec:java -Dexec.mainClass="wy.diary.server.migration.SimpleMigrationTool"
 */
public class SimpleMigrationTool {
    
    // =================== 配置参数 ===================
    private static final String DB_URL = System.getProperty("DB_URL", 
        "jdbc:mysql://localhost:3306/diary_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true");
    private static final String DB_USERNAME = System.getProperty("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getProperty("DB_PASSWORD", "123456");
    private static final String JSON_STORAGE_PATH = System.getProperty("JSON_STORAGE_PATH", "diary-server/diary/");
    
    // 是否为试运行模式（不实际插入数据库）
    private static final boolean DRY_RUN = Boolean.parseBoolean(System.getProperty("DRY_RUN", "false"));
    
    // =================== 统计信息 ===================
    private static final AtomicInteger totalProcessed = new AtomicInteger(0);
    private static final AtomicInteger totalSuccess = new AtomicInteger(0);
    private static final AtomicInteger totalSkipped = new AtomicInteger(0);
    private static final AtomicInteger totalFailed = new AtomicInteger(0);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("=== 日记数据一次性迁移工具 ===");
        System.out.println("数据库URL: " + DB_URL);
        System.out.println("用户名: " + DB_USERNAME);
        System.out.println("JSON存储路径: " + JSON_STORAGE_PATH);
        System.out.println("试运行模式: " + DRY_RUN);
        System.out.println();
        
        try {
            // 验证数据库连接
            if (!DRY_RUN) {
                validateDatabaseConnection();
            }
            
            // 扫描JSON文件
            List<File> jsonFiles = scanJsonFiles();
            if (jsonFiles.isEmpty()) {
                System.out.println("未找到任何JSON文件，迁移结束。");
                return;
            }
            
            System.out.printf("找到 %d 个JSON文件待迁移\n", jsonFiles.size());
            System.out.println();
            
            // 执行迁移
            executeMigration(jsonFiles);
            
            // 输出最终统计
            printFinalStatistics();
            
        } catch (Exception e) {
            System.err.println("迁移过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("=== 数据迁移完成 ===");
    }
    
    /**
     * 验证数据库连接
     */
    private static void validateDatabaseConnection() throws SQLException {
        System.out.println("验证数据库连接...");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            // 检查diary表是否存在
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'diary_db' AND table_name = 'diary'")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    throw new SQLException("数据库表 'diary' 不存在，请先创建表结构");
                }
            }
            System.out.println("数据库连接验证成功");
        }
    }
    
    /**
     * 扫描JSON文件
     */
    private static List<File> scanJsonFiles() throws IOException {
        List<File> jsonFiles = new ArrayList<>();
        Path storageDir = Paths.get(JSON_STORAGE_PATH);
        
        if (!Files.exists(storageDir)) {
            System.out.println("警告: JSON存储目录不存在: " + JSON_STORAGE_PATH);
            return jsonFiles;
        }
        
        try {
            Files.walk(storageDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                .forEach(path -> jsonFiles.add(path.toFile()));
        } catch (IOException e) {
            throw new IOException("扫描JSON文件时发生错误: " + e.getMessage(), e);
        }
        
        jsonFiles.sort(Comparator.comparing(File::getName));
        return jsonFiles;
    }
    
    /**
     * 执行迁移
     */
    private static void executeMigration(List<File> jsonFiles) throws SQLException, IOException {
        try (Connection conn = DRY_RUN ? null : DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            if (!DRY_RUN) {
                conn.setAutoCommit(false); // 启用事务
            }
            
            for (int i = 0; i < jsonFiles.size(); i++) {
                File jsonFile = jsonFiles.get(i);
                System.out.printf("正在处理文件 %d/%d: %s\n", i + 1, jsonFiles.size(), jsonFile.getName());
                
                try {
                    MigrationResult result = migrateFile(conn, jsonFile);
                    System.out.printf("文件 %s 迁移完成 - 处理: %d, 成功: %d, 跳过: %d, 失败: %d\n",
                        jsonFile.getName(), result.processed, result.success, result.skipped, result.failed);
                    System.out.println();
                    
                } catch (Exception e) {
                    System.err.printf("处理文件 %s 时发生错误: %s\n", jsonFile.getName(), e.getMessage());
                    totalFailed.incrementAndGet();
                }
            }
            
            if (!DRY_RUN && conn != null) {
                conn.commit();
                System.out.println("所有更改已提交到数据库");
            }
        }
    }
    
    /**
     * 从文件名中提取openId
     * 文件名格式: {openid}-年份.json
     */
    private static String extractOpenIdFromFileName(String fileName) {
        try {
            // 移除.json后缀
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            // 查找最后一个'-'的位置
            int lastDashIndex = nameWithoutExt.lastIndexOf('-');
            if (lastDashIndex > 0) {
                return nameWithoutExt.substring(0, lastDashIndex);
            }
        } catch (Exception e) {
            System.err.printf("  警告: 无法从文件名 %s 中提取openId: %s\n", fileName, e.getMessage());
        }
        return "";
    }

    /**
     * 迁移单个文件
     */
    private static MigrationResult migrateFile(Connection conn, File jsonFile) throws IOException, SQLException {
        MigrationResult result = new MigrationResult();
        
        // 从文件名中提取openId
        String openId = extractOpenIdFromFileName(jsonFile.getName());
        if (openId.isEmpty()) {
            System.out.printf("  警告: 无法从文件名 %s 中提取openId，跳过此文件\n", jsonFile.getName());
            return result;
        }
        
        // 读取JSON文件内容
        String content = Files.readString(jsonFile.toPath());
        if (content.trim().isEmpty()) {
            System.out.println("  跳过空文件");
            return result;
        }
        
        try {
            JSONObject jsonData = new JSONObject(content);
            
            // 处理JSON数据中的每个日记条目
            for (String key : jsonData.keySet()) {
                result.processed++;
                totalProcessed.incrementAndGet();
                
                try {
                    JSONObject diaryJson = jsonData.getJSONObject(key);
                    
                    // 检查是否已存在
                    String diaryId = diaryJson.optString("diaryId", key);
                    if (!DRY_RUN && isDiaryExists(conn, diaryId)) {
                        result.skipped++;
                        totalSkipped.incrementAndGet();
                        continue;
                    }
                    
                    if (DRY_RUN) {
                        System.out.printf("  [试运行] 将迁移日记: %s (openId: %s)\n", diaryId, openId);
                        result.success++;
                        totalSuccess.incrementAndGet();
                    } else {
                        // 插入到数据库
                        insertDiary(conn, diaryJson, diaryId, openId);
                        result.success++;
                        totalSuccess.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    System.err.printf("  处理日记条目失败: %s\n", e.getMessage());
                    result.failed++;
                    totalFailed.incrementAndGet();
                }
            }
            
        } catch (Exception e) {
            throw new IOException("解析JSON文件失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 检查日记是否已存在
     */
    private static boolean isDiaryExists(Connection conn, String diaryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM diary WHERE diary_id = ? AND deleted = 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, diaryId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
    
    /**
     * 插入日记到数据库
     */
    private static void insertDiary(Connection conn, JSONObject diaryJson, String diaryId, String openId) throws SQLException {
        String sql = "INSERT INTO diary (diary_id, open_id, editor_content, create_time, log_time, " +
                    "log_week, log_lunar, address, image_urls, db_create_time, db_update_time, deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, diaryId);
            stmt.setString(2, openId); // 使用从文件名提取的openId
            stmt.setString(3, diaryJson.optString("editorContent", ""));
            stmt.setString(4, diaryJson.optString("createTime", ""));
            stmt.setString(5, diaryJson.optString("logTime", ""));
            stmt.setString(6, diaryJson.optString("logWeek", ""));
            stmt.setString(7, diaryJson.optString("logLunar", ""));
            stmt.setString(8, diaryJson.optString("address", ""));
            
            // 处理imageUrls - 可能是数组格式，转换为JSON字符串
            String imageUrls = "";
            if (diaryJson.has("imageUrls")) {
                Object imageUrlsObj = diaryJson.get("imageUrls");
                if (imageUrlsObj instanceof org.json.JSONArray) {
                    imageUrls = imageUrlsObj.toString();
                } else {
                    imageUrls = diaryJson.optString("imageUrls", "");
                }
            }
            stmt.setString(9, imageUrls);
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * 输出最终统计
     */
    private static void printFinalStatistics() {
        System.out.println("=== 迁移统计 ===");
        System.out.printf("处理日记条目总数: %d\n", totalProcessed.get());
        System.out.printf("成功迁移: %d\n", totalSuccess.get());
        System.out.printf("跳过(已存在): %d\n", totalSkipped.get());
        System.out.printf("失败: %d\n", totalFailed.get());
        
        if (DRY_RUN) {
            System.out.println("注意: 这是试运行模式，未实际写入数据库");
        }
    }
    
    /**
     * 迁移结果统计
     */
    private static class MigrationResult {
        int processed = 0;
        int success = 0;
        int skipped = 0;
        int failed = 0;
    }
} 