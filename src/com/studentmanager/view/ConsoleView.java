
package com.studentmanager.view;

import com.studentmanager.model.Student;
import com.studentmanager.service.Analytics;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class ConsoleView {

    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleView(Scanner scanner, PrintStream out) {
        this.scanner = Objects.requireNonNull(scanner);
        this.out = Objects.requireNonNull(out);
    }

    // 读取一行用户输入；输入流结束则返回null
    public String ask(String prompt) {
        out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    public void message(String text) {
        out.println(text);
    }

    public void error(String text) {
        out.println("错误：" + text);
    }

    // 显示帮助菜单
    public void showHelp() {
        out.println("""

                ===== 学生成绩管理系统 =====
                help          查看帮助
                list          显示所有学生
                add           添加学生
                find          按学号查询
                update        修改学生信息
                delete        删除学生
                top [N]       成绩前N名
                stats         全校成绩统计
                passing       显示及格学生
                classes       显示全部班级
                class <班级>  指定班级统计
                report        导出成绩报告
                save          保存CSV
                exit          保存并退出
                ============================
                """);
    }

    // 展示学生列表
    public void showStudents(List<Student> students) {
        if (students.isEmpty()) {
            out.println("暂无学生");
            return;
        }

        out.println("学号\t姓名\t班级\t成绩");

        for (Student s : students) {
            out.printf(Locale.ROOT, "%s\t%s\t%s\t%.1f%n",
                    s.id(), s.name(), s.className(), s.score());
        }
    }

    // 展示班级名称
    public void showClasses(List<String> classes) {
        if (classes.isEmpty()) {
            out.println("暂无班级");
            return;
        }

        for (String name : classes) {
            out.println(name);
        }
    }

    // 全校统计展示
    public void showStats(Analytics.Statistics s) {
        out.println("===== 全校成绩统计 =====");

        out.println("总人数：" + s.totalCount());
        out.println("及格人数：" + s.passingCount());

        out.printf(Locale.ROOT, "总分：%.3f%n",
                s.totalScore());
        out.printf(Locale.ROOT, "平均分：%.3f%n",
                s.averageScore());
        out.printf(Locale.ROOT, "及格率：%.2f%%%n",
                s.passingRatePercent());

        s.highest().ifPresentOrElse(
                student -> out.println(
                        "最高分学生：" + student.name()
                                + "（" + student.id() + "）"),
                () -> out.println("最高分学生：无"));

        out.println("各班人数及平均分：");

        for (Map.Entry<String, Long> entry : s.classCounts().entrySet()) {

            String name = entry.getKey();
            double avg = s.classAverages().get(name);

            out.printf(Locale.ROOT,
                    "%s：%d人，平均分%.3f%n",
                    name, entry.getValue(), avg);
        }
    }

    // 指定班级统计展示
    public void showClassStats(Analytics.ClassStatistics s) {
        out.println("班级：" + s.className());
        out.println("人数：" + s.totalCount());

        out.printf(Locale.ROOT, "平均分：%.3f%n",
                s.averageScore());
        out.printf(Locale.ROOT, "及格率：%.2f%%%n",
                s.passingRatePercent());
    }
}
