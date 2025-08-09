package wy.diary.server.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import wy.diary.server.ServerApplication;
import wy.diary.server.service.BackupService;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ServerApplication.class)
public class BackupServiceTest {

    @Autowired
    private BackupService backupService;

    @Test
    public void testCreateBackup() throws Exception {
        // 执行备份
        backupService.createBackup();
        
        // 检查备份目录
        File backupDir = new File("diary-server/backups/");
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("fish-diary-") && name.endsWith(".zip"));
        
        // 验证是否成功创建了备份文件
        assertTrue(backupFiles != null && backupFiles.length > 0, "备份文件应该被创建");
        
        // 打印备份文件信息
        System.out.println("创建的备份文件: " + backupFiles[0].getName());
        System.out.println("备份文件大小: " + (backupFiles[0].length() / 1024) + " KB");
    }
}