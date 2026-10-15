
package com.studentmanager;

import com.studentmanager.controller.StudentController;
import com.studentmanager.repository.StudentRepository;
import com.studentmanager.service.Analytics;
import com.studentmanager.service.ReportService;
import com.studentmanager.view.ConsoleView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // 1. 创建Repository
        StudentRepository repository = new StudentRepository(
                Path.of("data", "students.csv"));

        // 2. 启动时读取CSV
        try {
            repository.load();
        } catch (IOException e) {
            System.err.println(
                    "启动失败，CSV无法加载：" + e.getMessage());
            System.err.println(
                    "请先修复原始文件，程序不会覆盖它。");
            return;
        }

        // 3. 创建业务服务
        Analytics analytics = new Analytics(repository);

        ReportService reportService = new ReportService(
                analytics,
                Path.of("data", "report.txt"));

        // 4. 创建View
        ConsoleView view = new ConsoleView(
                new Scanner(System.in, StandardCharsets.UTF_8),
                System.out);

        // 5. 创建Controller
        StudentController controller = new StudentController(
                view,
                repository,
                analytics,
                reportService);

        // 6. 启动命令行交互
        controller.run();
    }
}
