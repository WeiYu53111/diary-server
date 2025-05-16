package wy.diary.server.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public class BackupCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(BackupCleanupTask.class);

    @Value("${backup.temp.directory:${java.io.tmpdir}/fish-diary-backups}")
    private String tempBackupDirectory;

    @Value("${clean.cron}")
    private String cleanCron;

    /**
     * 每天凌晨1点执行清理任务
     */
    @Scheduled(cron = "${clean.cron}")
    public void cleanupTempBackupFiles() {
        logger.info("开始清理临时备份文件...");
        File tempDir = new File(tempBackupDirectory);

        if (!tempDir.exists()) {
            logger.info("临时备份目录不存在，无需清理");
            return;
        }

        try (Stream<Path> paths = Files.walk(tempDir.toPath())) {
            // 先删除文件，再删除目录
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (file.delete()) {
                            logger.debug("已删除: {}", file.getAbsolutePath());
                        } else {
                            logger.warn("无法删除: {}", file.getAbsolutePath());
                        }
                    });

            // 重新创建临时目录
            tempDir.mkdirs();
            logger.info("临时备份文件清理完成");
        } catch (IOException e) {
            logger.error("清理临时备份文件时出错", e);
        }
    }
}