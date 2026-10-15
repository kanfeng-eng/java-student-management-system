// 测试 Student 是否正确

package com.studentmanager.test;

import com.studentmanager.model.Student;

public class AcceptanceTest {

    private static int passed = 0;

    public static void main(String[] args) {
        Student s = new Student(
                " S001 ", " 张三 ", "软件1班", 92);

        // 正常数据
        check(s.id().equals("S001"), "学号去空格");
        check(s.name().equals("张三"), "姓名去空格");
        check(s.className().equals("软件1班"), "班级正确");
        check(s.score() == 92, "成绩正确");

        // record 自动生成的 equals()
        Student same = new Student(
                "S001", "张三", "软件1班", 92);
        check(s.equals(same), "record 值相等");

        // 成绩边界
        check(new Student("S002", "李四", "1班", 0)
                .score() == 0, "允许0分");
        check(new Student("S003", "王五", "1班", 100)
                .score() == 100, "允许100分");

        // 非法数据
        reject("空白学号",
                () -> new Student("  ", "张三", "1班", 90));
        reject("姓名为null",
                () -> new Student("S001", null, "1班", 90));
        reject("班级包含逗号",
                () -> new Student("S001", "张三", "1,班", 90));
        reject("姓名包含换行",
                () -> new Student("S001", "张\n三", "1班", 90));
        reject("负数成绩",
                () -> new Student("S001", "张三", "1班", -1));
        reject("成绩超过100",
                () -> new Student("S001", "张三", "1班", 101));
        reject("NaN成绩",
                () -> new Student("S001", "张三", "1班",
                        Double.NaN));
        reject("无穷大成绩",
                () -> new Student("S001", "张三", "1班",
                        Double.POSITIVE_INFINITY));

        System.out.println("总计通过：" + passed + " 项测试");
    }

    // message是用于检验的方法
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("失败：" + message);
        }
        passed++;
        System.out.println("通过：" + message);
    }

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
