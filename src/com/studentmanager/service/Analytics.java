
package com.studentmanager.service;

import com.studentmanager.model.Student;
import com.studentmanager.repository.StudentRepository;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Analytics {

    private static final double PASS_LINE = 60.0;

    // 成绩降序；同分按学号升序
    private static final Comparator<Student> RANKING = Comparator.comparingDouble(Student::score)
            .reversed()
            .thenComparing(Student::id);

    // Lambda + Predicate 函数式接口
    private static final Predicate<Student> IS_PASSING = s -> s.score() >= PASS_LINE;

    private final StudentRepository repository;

    public Analytics(StudentRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    // 1. 成绩前N名
    public List<Student> top(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("N不能小于0");
        }

        return repository.findAll().stream()
                .sorted(RANKING)
                .limit(n)
                .toList();
    }

    // 2. 所有及格学生，按成绩降序
    public List<Student> passing() {
        return repository.findAll().stream()
                .filter(IS_PASSING)
                .sorted(RANKING)
                .toList();
    }

    // 3. 班级名称：去重 + 排序
    public List<String> classes() {
        return repository.findAll().stream()
                .map(Student::className)
                .distinct()
                .sorted()
                .toList();
    }

    // 4. 筛选指定班级学生
    public List<Student> classStudents(String className) {
        String target = checkClassName(className);

        return repository.findAll().stream()
                .filter(s -> s.className().equals(target))
                .sorted(RANKING)
                .toList();
    }

    // 5. 指定班级统计
    public ClassStatistics classStats(String className) {
        String target = checkClassName(className);
        List<Student> group = classStudents(target);

        int count = group.size();

        double average = group.stream()
                .mapToDouble(Student::score)
                .average()
                .orElse(0.0);

        long passingCount = group.stream()
                .filter(IS_PASSING)
                .count();

        double rate = count == 0
                ? 0.0
                : passingCount * 100.0 / count;

        return new ClassStatistics(
                target, count, average, rate);
    }

    // 6. 全校成绩统计
    public Statistics stats() {
        List<Student> all = repository.findAll();

        int total = all.size();

        long passingCount = all.stream()
                .filter(IS_PASSING)
                .count();

        double totalScore = all.stream()
                .mapToDouble(Student::score)
                .reduce(0.0, Double::sum);

        double average = total == 0
                ? 0.0
                : totalScore / total;

        double rate = total == 0
                ? 0.0
                : passingCount * 100.0 / total;

        // RANKING的第一个元素就是成绩最高者
        Optional<Student> highest = all.stream()
                .min(RANKING);

        // 各班人数
        Map<String, Long> classCounts = all.stream()
                .collect(Collectors.groupingBy(
                        Student::className,
                        TreeMap::new,
                        Collectors.counting()));

        // 各班平均分
        Map<String, Double> classAverages = all.stream()
                .collect(Collectors.groupingBy(
                        Student::className,
                        TreeMap::new,
                        Collectors.averagingDouble(Student::score)));

        return new Statistics(
                total,
                passingCount,
                totalScore,
                average,
                rate,
                highest,
                Collections.unmodifiableMap(classCounts),
                Collections.unmodifiableMap(classAverages));
    }

    private static String checkClassName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("班级名称不能为空");
        }
        return name.strip();
    }

    // 7. 全校统计结果的数据模型
    public record Statistics(
            int totalCount,
            long passingCount,
            double totalScore,
            double averageScore,
            double passingRatePercent,
            Optional<Student> highest,
            Map<String, Long> classCounts,
            Map<String, Double> classAverages) {
    }

    // 8. 班级统计结果的数据模型
    public record ClassStatistics(
            String className,
            int totalCount,
            double averageScore,
            double passingRatePercent) {
    }
}
