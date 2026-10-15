
package com.studentmanager.test;

import com.studentmanager.model.Student;
import com.studentmanager.repository.StudentRepository;
import com.studentmanager.service.Analytics;

import java.nio.file.Path;
import java.util.List;

public class AcceptanceTest_3 {

    private static int passed = 0;

    public static void main(String[] args) {
        StudentRepository repo = new StudentRepository(Path.of("unused-test.csv"));

        // 构造8条测试数据，不读写真实文件
        repo.add(new Student("S001", "张三", "软件1班", 92));
        repo.add(new Student("S002", "李四", "软件1班", 57));
        repo.add(new Student("S003", "王五", "软件2班", 85));
        repo.add(new Student("S004", "赵六", "软件2班", 71));
        repo.add(new Student("S005", "钱七", "软件1班", 76));
        repo.add(new Student("S006", "孙八", "软件2班", 100));
        repo.add(new Student("S007", "周九", "软件1班", 60));
        repo.add(new Student("S008", "吴十", "软件2班", 48));

        Analytics a = new Analytics(repo);

        // 1. 排名
        check(a.top(3).stream().map(Student::id).toList()
                .equals(List.of("S006", "S001", "S003")),
                "前三名排序正确");

        check(a.top(0).isEmpty(), "Top 0返回空列表");
        check(a.top(100).size() == 8, "Top超过总人数");

        // 2. 及格学生
        check(a.passing().size() == 6, "及格人数正确");
        check(a.passing().get(0).id().equals("S006"),
                "及格学生按成绩降序");

        // 3. 班级
        check(a.classes().equals(
                List.of("软件1班", "软件2班")), "班级去重排序");

        check(a.classStudents("软件1班").size() == 4,
                "班级筛选正确");

        check(a.classStudents("不存在班级").isEmpty(),
                "不存在班级返回空列表");

        // 4. 全校统计
        Analytics.Statistics s = a.stats();

        check(s.totalCount() == 8, "总人数8");
        close(s.totalScore(), 589.0, "总分589");
        close(s.averageScore(), 73.625, "平均分73.625");
        check(s.passingCount() == 6, "及格人数6");
        close(s.passingRatePercent(), 75.0, "及格率75%");
        check(s.highest().orElseThrow().id().equals("S006"),
                "最高分学生正确");

        check(s.classCounts().get("软件1班") == 4L,
                "软件1班人数正确");
        check(s.classCounts().get("软件2班") == 4L,
                "软件2班人数正确");

        close(s.classAverages().get("软件1班"), 71.25,
                "软件1班平均分");
        close(s.classAverages().get("软件2班"), 76.0,
                "软件2班平均分");

        // 5. 指定班级统计
        Analytics.ClassStatistics c = a.classStats("软件1班");

        check(c.totalCount() == 4, "班级统计人数");
        close(c.averageScore(), 71.25, "班级统计平均分");
        close(c.passingRatePercent(), 75.0, "班级及格率");

        // 6. 空数据
        Analytics empty = new Analytics(
                new StudentRepository(Path.of("unused-empty.csv")));

        check(empty.stats().totalCount() == 0, "空数据总人数");
        check(empty.stats().highest().isEmpty(), "空数据无最高分");
        close(empty.stats().averageScore(), 0.0, "空数据平均分");
        check(empty.top(3).isEmpty(), "空数据排名");
        check(empty.stats().classCounts().isEmpty(), "空数据分组");

        // 7. 错误输入
        reject(() -> a.top(-1), "负数Top报错");
        reject(() -> a.classStats(null), "空班级名报错");

        // 8. 同分按学号排序
        StudentRepository ties = new StudentRepository(Path.of("unused-tie.csv"));
        ties.add(new Student("S020", "甲", "1班", 90));
        ties.add(new Student("S010", "乙", "1班", 90));

        Analytics tieAnalytics = new Analytics(ties);

        check(tieAnalytics.top(2).get(0).id().equals("S010"),
                "同分学号升序");

        System.out.println("阶段3通过：" + passed + "项测试");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("失败：" + message);
        }
        passed++;
        System.out.println("通过：" + message);
    }

    private static void close(double actual, double expected,
            String message) {
        check(Math.abs(actual - expected) < 1e-9, message);
    }

    private static void reject(Runnable action, String message) {
        boolean thrown = false;
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        check(thrown, message);
    }
}
