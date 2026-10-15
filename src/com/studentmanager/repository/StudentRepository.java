package com.studentmanager.repository;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import com.studentmanager.model.Student;

public class StudentRepository {
    private final Path csvPath;
    private boolean saveAllowed = true;

    public StudentRepository(Path csvPath) {
        this.csvPath = Objects.requireNonNull(csvPath)
                .toAbsolutePath().normalize();
    }

    private final Map<String, Student> students = new LinkedHashMap<>();

    public void add(Student s) {
        Objects.requireNonNull(s, "学生对象不能为空");
        if (students.containsKey(s.id())) {
            throw new IllegalArgumentException("学号已经存在");
        }
        students.put(s.id(), s);
    }

    public Optional<Student> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(students.get(id.strip()));
    }

    public void update(Student student) {
        Objects.requireNonNull(student, "学生对象不能为空");

        if (!students.containsKey(student.id())) {
            throw new IllegalArgumentException("学号不存在");
        }

        students.replace(student.id(), student);
    }

    public boolean delete(String id) {
        if (id == null) {
            return false;
        }
        return students.remove(id.strip()) != null;
    }

    public List<Student> findAll() {
        return List.copyOf(students.values());
    }

    public void load() throws IOException {
        Map<String, Student> loaded = new LinkedHashMap<>();

        try {
            if (Files.notExists(csvPath)) {
                students.clear();
                saveAllowed = true;
                return;
            }

            try (BufferedReader reader = Files.newBufferedReader(
                    csvPath, StandardCharsets.UTF_8)) {

                String header = reader.readLine();

                if (!"id,name,className,score".equals(header)) {
                    throw new IOException("CSV第1行：表头错误");
                }

                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    String[] parts = line.split(",", -1);

                    if (parts.length != 4) {
                        throw new IOException(
                                "CSV第" + lineNumber + "行：字段数必须为4");
                    }

                    Student student;

                    try {
                        double score = Double.parseDouble(parts[3]);

                        student = new Student(
                                parts[0], parts[1], parts[2], score);

                    } catch (IllegalArgumentException e) {
                        throw new IOException(
                                "CSV第" + lineNumber + "行：" + e.getMessage(),
                                e);
                    }

                    if (loaded.putIfAbsent(
                            student.id(), student) != null) {
                        throw new IOException(
                                "CSV第" + lineNumber + "行：学号重复");
                    }
                }
            }
        } catch (IOException e) {
            saveAllowed = false;
            throw e;
        }

        students.clear();
        students.putAll(loaded);
        saveAllowed = true;
    }

    public void save() throws IOException {
        if (!saveAllowed) {
            throw new IllegalStateException(
                    "上次加载CSV失败，禁止覆盖原文件");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("id,name,className,score\n");

        for (Student s : students.values()) {
            sb.append(s.id()).append(",")
                    .append(s.name()).append(",")
                    .append(s.className()).append(",")
                    .append(s.score()).append("\n");
        }

        Path parent = csvPath.getParent();
        Files.createDirectories(parent);

        Path temp = Files.createTempFile(
                parent, "students-", ".tmp");

        try {
            Files.writeString(
                    temp, sb.toString(), StandardCharsets.UTF_8);

            try {
                Files.move(temp, csvPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, csvPath,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

}
