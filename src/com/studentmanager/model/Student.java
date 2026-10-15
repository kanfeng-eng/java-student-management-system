// 本文件 用于定义学生的数据结构，检查数据合法性

package com.studentmanager.model;

// Java 21 之后可以使用 record  自动 -> 构造方法  访问方法 equals() hashCode() toString()

public record Student(
        String id,
        String name,
        String className,
        double score) {
    // record 的紧凑构造方法 -> 处理数据，写入主体
    public Student {
        // 这里的 id 不是 this.id，而是构造方法的参数（局部变量）
        id = validateText(id, "学号");
        name = validateText(name, "姓名");
        className = validateText(className, "班级");

        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new IllegalArgumentException(
                    "成绩必须是0到100之间的有限数字");
        }
    }

    // 公共的文本校验逻辑
    private static String validateText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "不能为空");
        }

        if (value.contains(",")
                || value.contains("\r")
                || value.contains("\n")) {
            throw new IllegalArgumentException(
                    field + "不能包含逗号或换行符");
        }

        return value.strip();
    }
}
