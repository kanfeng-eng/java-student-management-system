
package com.studentmanager.controller;

import com.studentmanager.model.Student;
import com.studentmanager.repository.StudentRepository;
import com.studentmanager.service.Analytics;
import com.studentmanager.service.ReportService;
import com.studentmanager.view.ConsoleView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

public class StudentController {

    private final ConsoleView view;
    private final StudentRepository repository;
    private final Analytics analytics;
    private final ReportService reportService;

    public StudentController(
            ConsoleView view,
            StudentRepository repository,
            Analytics analytics,
            ReportService reportService) {

        this.view = Objects.requireNonNull(view);
        this.repository = Objects.requireNonNull(repository);
        this.analytics = Objects.requireNonNull(analytics);
        this.reportService = Objects.requireNonNull(reportService);
    }

    // 主命令循环
    public void run() {
        view.message("欢迎使用 Student Grade Manager");
        view.showHelp();

        while (true) {
            String line = view.ask("\n命令> ");

            if (line == null) {
                view.error("输入已结束，未自动保存，请使用save或exit保存");
                return;
            }

            if (line.isBlank()) {
                continue;
            }

            try {
                boolean keepRunning = execute(line.strip());

                if (!keepRunning) {
                    return;
                }

            } catch (NoSuchElementException e) {
                view.error("输入已结束，操作中止，未自动保存");
                return;

            } catch (IllegalArgumentException
                    | IllegalStateException e) {
                view.error(e.getMessage());

            } catch (IOException e) {
                view.error("文件操作失败：" + e.getMessage());
            }
        }
    }

    // 命令分发
    private boolean execute(String line) throws IOException {
        String[] parts = line.split("\\s+", 2);

        String command = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length == 2
                ? parts[1].strip()
                : "";

        switch (command) {
            case "help" -> view.showHelp();

            case "list" ->
                view.showStudents(repository.findAll());

            case "add" -> addStudent();

            case "find" -> findStudent();

            case "update" -> updateStudent();

            case "delete" -> deleteStudent();

            case "top" -> {
                String value = argument.isEmpty()
                        ? ask("请输入N：")
                        : argument;

                int n = parseInteger(value);

                view.message("排行前" + n + "名：");
                view.showStudents(analytics.top(n));
            }

            case "stats" ->
                view.showStats(analytics.stats());

            case "passing" ->
                view.showStudents(analytics.passing());

            case "classes" ->
                view.showClasses(analytics.classes());

            case "class" -> {
                if (argument.isBlank()) {
                    throw new IllegalArgumentException(
                            "用法：class <班级>");
                }

                view.showClassStats(
                        analytics.classStats(argument));

                view.showStudents(
                        analytics.classStudents(argument));
            }

            case "report" -> {
                var path = reportService.generate();
                view.message("报告已导出：" + path);
            }

            case "save" -> {
                repository.save();
                view.message("保存成功");
            }

            case "exit" -> {
                repository.save();
                view.message("保存成功，退出程序");
                return false;
            }

            default -> view.error(
                    "未知命令，请输入help查看菜单");
        }

        return true;
    }

    // 添加学生
    private void addStudent() {
        String id = ask("学号：");
        String name = ask("姓名：");
        String className = ask("班级：");
        double score = parseScore(ask("成绩："));

        Student student = new Student(id, name, className, score);

        repository.add(student);

        view.message("添加成功");
    }

    // 查找学生
    private void findStudent() {
        String id = ask("学号：");

        repository.findById(id).ifPresentOrElse(
                student -> view.showStudents(List.of(student)),
                () -> view.message("未找到学生"));
    }

    // 修改学生：学号保持不变
    private void updateStudent() {
        String id = ask("要修改的学号：");

        Student old = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("学号不存在"));

        String name = ask("新姓名（留空不修改）：");
        String className = ask("新班级（留空不修改）：");
        String scoreText = ask("新成绩（留空不修改）：");

        if (name.isEmpty()) {
            name = old.name();
        }

        if (className.isEmpty()) {
            className = old.className();
        }

        double score = scoreText.isEmpty()
                ? old.score()
                : parseScore(scoreText);

        Student updated = new Student(
                old.id(), name, className, score);

        repository.update(updated);

        view.message("修改成功");
    }

    // 删除学生
    private void deleteStudent() {
        String id = ask("要删除的学号：");

        boolean deleted = repository.delete(id);

        view.message(deleted
                ? "删除成功"
                : "删除失败：学号不存在");
    }

    // 保证输入未提前结束
    private String ask(String prompt) {
        String value = view.ask(prompt);

        if (value == null) {
            throw new NoSuchElementException("输入已结束");
        }

        return value.strip();
    }

    // 成绩输入检查
    private double parseScore(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "成绩必须是数字", e);
        }
    }

    // Top N 输入检查
    private int parseInteger(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "N必须是整数", e);
        }
    }
}
