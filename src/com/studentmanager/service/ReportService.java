
package com.studentmanager.service;

import com.studentmanager.model.Student;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ReportService {

    private final Analytics analytics;
    private final Path reportPath;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReportService(Analytics analytics, Path reportPath) {
        this.analytics = Objects.requireNonNull(analytics);
        this.reportPath = Objects.requireNonNull(reportPath);
    }

    public Path generate() throws IOException {
        Analytics.Statistics s = analytics.stats();

        StringBuilder sb = new StringBuilder();

        sb.append("学生成绩分析报告\n");
        sb.append("生成时间：")
                .append(LocalDateTime.now().format(FORMAT))
                .append("\n\n");

        sb.append("总人数：").append(s.totalCount()).append("\n");
        sb.append("及格人数：").append(s.passingCount()).append("\n");

        sb.append(String.format(Locale.ROOT,
                "总分：%.3f%n", s.totalScore()));
        sb.append(String.format(Locale.ROOT,
                "平均分：%.3f%n", s.averageScore()));
        sb.append(String.format(Locale.ROOT,
                "及格率：%.2f%%%n", s.passingRatePercent()));

        if (s.highest().isPresent()) {
            Student student = s.highest().get();
            sb.append("最高分学生：")
                    .append(student.name())
                    .append("（").append(student.id()).append("）")
                    .append("\n");
        } else {
            sb.append("最高分学生：无\n");
        }

        sb.append("\n各班统计：\n");

        for (Map.Entry<String, Long> entry : s.classCounts().entrySet()) {

            String name = entry.getKey();

            sb.append(String.format(Locale.ROOT,
                    "%s：%d人，平均分%.3f%n",
                    name,
                    entry.getValue(),
                    s.classAverages().get(name)));
        }

        // 先写临时文件，再替换报告
        Path target = reportPath.toAbsolutePath().normalize();
        Path parent = target.getParent();

        Files.createDirectories(parent);

        Path temp = Files.createTempFile(
                parent, "report-", ".tmp");

        try {
            Files.writeString(
                    temp, sb.toString(), StandardCharsets.UTF_8);

            try {
                Files.move(temp, target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }

        return target;
    }
}
