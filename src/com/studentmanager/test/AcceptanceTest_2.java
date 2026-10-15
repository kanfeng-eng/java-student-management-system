
package com.studentmanager.test;

import com.studentmanager.model.Student;
import com.studentmanager.repository.StudentRepository;

import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AcceptanceTest_2 {

    private static int passed = 0;

    public static void main(String[] args) throws IOException {
        testRepository();
        System.out.println("阶段2测试完成，通过：" + passed + " 项");
    }

    private static void testRepository() throws IOException {

        // 创建临时测试文件，避免修改真实数据
        Path dir = Files.createTempDirectory("student-test-");
        Path csv = dir.resolve("students.csv");

        try {
            StudentRepository repo = new StudentRepository(csv);

            // 1. 加载空仓库
            repo.load();
            check(repo.findAll().isEmpty(), "空仓库加载");

            // 2. 添加学生
            Student s1 = new Student("S001", "张三", "1班", 92);
            Student s2 = new Student("S002", "李四", "1班", 58);

            repo.add(s1);
            repo.add(s2);

            check(repo.findAll().size() == 2, "添加学生");

            // 3. 查询学生
            check(repo.findById("S001").orElseThrow()
                    .name().equals("张三"), "按学号查询");

            check(repo.findById("S999").isEmpty(),
                    "查询不存在的学生");

            // 4. 重复学号
            reject("拒绝重复学号", () -> repo.add(new Student("S001", "王五", "2班", 80)));

            // 5. 修改学生
            repo.update(new Student("S001", "张三", "1班", 95));

            check(repo.findById("S001").orElseThrow()
                    .score() == 95, "修改成绩");

            reject("拒绝修改不存在的学号", () -> repo.update(new Student("S999", "王五", "2班", 80)));

            // 6. 删除学生
            List<Student> snapshot = repo.findAll();

            check(repo.delete("S002"), "删除已有学生");
            check(!repo.delete("S999"), "删除不存在的学生");

            // 7. 集合封装
            check(snapshot.size() == 2, "列表是独立快照");

            try {
                snapshot.clear();
                throw new AssertionError("列表应该不可修改");
            } catch (UnsupportedOperationException e) {
                check(true, "列表不可修改");
            }

            // 8. 文件保存和读取
            repo.save();

            StudentRepository reloaded = new StudentRepository(csv);
            reloaded.load();

            check(reloaded.findAll().size() == 1, "CSV重新加载");

            check(reloaded.findById("S001").orElseThrow()
                    .score() == 95, "保存后数据一致");

            // 9. CSV重复学号
            String badCsv = """
                    id,name,className,score
                    S001,张三,1班,92
                    S001,王五,2班,80
                    """;

            Files.writeString(csv, badCsv, StandardCharsets.UTF_8);

            try {
                reloaded.load();
                throw new AssertionError("应拒绝重复学号");
            } catch (IOException e) {
                check(e.getMessage().contains("第3行"),
                        "CSV错误行号");
            }

            // 10. 加载失败后的数据保护
            check(reloaded.findAll().size() == 1,
                    "加载失败不破坏内存数据");

            try {
                reloaded.save();
                throw new AssertionError("应禁止保存");
            } catch (IllegalStateException e) {
                check(true, "加载失败禁止保存");
            }

            check(Files.readString(csv, StandardCharsets.UTF_8)
                    .equals(badCsv), "损坏的CSV未被覆盖");

            // 11. 非法数字
            Files.writeString(csv, """
                    id,name,className,score
                    S003,赵六,2班,abc
                    """, StandardCharsets.UTF_8);

            try {
                reloaded.load();
                throw new AssertionError("应拒绝非法成绩");
            } catch (IOException e) {
                check(e.getMessage().contains("第2行"),
                        "非法数字报告行号");
            }

        } finally {
            Files.deleteIfExists(csv);
            Files.deleteIfExists(dir);
        }
    }

    // 普通断言
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("失败：" + message);
        }

        passed++;
        System.out.println("通过：" + message);
    }

    // 检查是否抛出预期异常
    private static void reject(String message, Runnable action) {
        boolean thrown = false;

        try {
            action.run();
        } catch (IllegalArgumentException e) {
            thrown = true;
        }

        check(thrown, message);
    }
}
