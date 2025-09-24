/*
README.md
Campus Course & Records Manager (CCRM) - Starter Project

This repository is a starter scaffold for the Campus Course & Records Manager Java SE console application.
It contains:
 - README with notes required by the assignment (Java evolution, JDK/JRE/JVM, install steps, screenshots placeholders)
 - Package skeleton (edu.ccrm.*)
 - Key domain classes (Person, Student, Instructor, Course, Enrollment, Semester, Grade)
 - Services (StudentService, CourseService, EnrollmentService) with simple in-memory implementations
 - I/O skeleton using NIO.2: ImportExportService
 - AppConfig singleton, DataStore singleton
 - CLI Main with menu-driven flow using switch (enhanced/classic) and loops
 - Examples of Builder pattern, Singleton, enums with fields, nested classes, lambdas, streams, recursion utility

HOW TO USE
1. Open this project in your IDE (Eclipse/IntelliJ) as a Java project.
2. Compile and run edu.ccrm.cli.CCRMApp (contains main()).
3. The CLI will present a menu. You can add students/courses, enroll, record grades, export, backup.

NOTE: This is a starter scaffold to help you implement the full project. Many methods have TODOs or minimal logic but show required patterns and structure.
*/

// ----------------------------
// File: src/edu/ccrm/config/AppConfig.java
// ----------------------------
package edu.ccrm.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public final class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    private final Path dataFolder;
    private final DateTimeFormatter backupFolderFormatter;

    private AppConfig() {
        // default config; could read from properties file
        this.dataFolder = Paths.get(System.getProperty("user.home"), "ccrm-data");
        this.backupFolderFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public DateTimeFormatter getBackupFolderFormatter() {
        return backupFolderFormatter;
    }
}

// ----------------------------
// File: src/edu/ccrm/domain/Semester.java
// ----------------------------
package edu.ccrm.domain;

public enum Semester {
    SPRING("Spring"), SUMMER("Summer"), FALL("Fall");

    private final String display;

    Semester(String display) { this.display = display; }

    public String getDisplay() { return display; }

    @Override
    public String toString() { return display; }
}

// ----------------------------
// File: src/edu/ccrm/domain/Grade.java
// ----------------------------
package edu.ccrm.domain;

public enum Grade {
    S(10), A(9), B(8), C(7), D(6), E(5), F(0);

    private final int points;
    Grade(int points) { this.points = points; }
    public int getPoints() { return points; }
}

// ----------------------------
// File: src/edu/ccrm/domain/Person.java
// ----------------------------
package edu.ccrm.domain;

import java.time.LocalDate;

public abstract class Person {
    protected final String id;
    protected String fullName;
    protected String email;
    protected LocalDate createdAt;

    protected Person(String id, String fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = LocalDate.now();
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }

    public abstract String getProfile();

    @Override
    public String toString() {
        return String.format("%s (%s) <%s>", fullName, id, email);
    }
}

// ----------------------------
// File: src/edu/ccrm/domain/Student.java
// ----------------------------
package edu.ccrm.domain;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Student extends Person {
    public enum Status { ACTIVE, INACTIVE }

    private String regNo;
    private Status status;
    private final Map<String, Enrollment> enrollments = new HashMap<>(); // courseCode -> enrollment
    private LocalDate dob;

    public Student(String id, String regNo, String fullName, String email) {
        super(id, fullName, email);
        this.regNo = regNo;
        this.status = Status.ACTIVE;
        this.dob = LocalDate.now().minusYears(18); // placeholder
    }

    public String getRegNo() { return regNo; }
    public Status getStatus() { return status; }
    public void deactivate() { this.status = Status.INACTIVE; }
    public Collection<Enrollment> getEnrollments() { return enrollments.values(); }

    public void enroll(Enrollment e) {
        enrollments.put(e.getCourse().getCode(), e);
    }

    public void unenroll(String courseCode) {
        enrollments.remove(courseCode);
    }

    @Override
    public String getProfile() {
        return String.format("Student: %s | RegNo: %s | Status: %s", fullName, regNo, status);
    }

    public String getTranscript() {
        StringBuilder sb = new StringBuilder();
        sb.append(getProfile()).append("\n");
        double totalPoints = 0; int totalCredits = 0;
        for (Enrollment e : enrollments.values()) {
            sb.append(e).append("\n");
            if (e.getGrade() != null) {
                totalPoints += e.getGrade().getPoints() * e.getCourse().getCredits();
                totalCredits += e.getCourse().getCredits();
            }
        }
        double gpa = totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
        sb.append(String.format("GPA: %.2f", gpa));
        return sb.toString();
    }
}

// ----------------------------
// File: src/edu/ccrm/domain/Instructor.java
// ----------------------------
package edu.ccrm.domain;

public class Instructor extends Person {
    private String department;

    public Instructor(String id, String fullName, String email, String department) {
        super(id, fullName, email);
        this.department = department;
    }

    @Override
    public String getProfile() {
        return String.format("Instructor: %s | Dept: %s", fullName, department);
    }
}

// ----------------------------
// File: src/edu/ccrm/domain/Course.java
// ----------------------------
package edu.ccrm.domain;

import java.util.Objects;

public final class Course {
    private final String code; // immutable
    private String title;
    private int credits;
    private Instructor instructor; // may be null
    private Semester semester;
    private String department;
    private boolean active = true;

    private Course(Builder b) {
        this.code = b.code;
        this.title = b.title;
        this.credits = b.credits;
        this.instructor = b.instructor;
        this.semester = b.semester;
        this.department = b.department;
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public Instructor getInstructor() { return instructor; }
    public Semester getSemester() { return semester; }
    public String getDepartment() { return department; }
    public boolean isActive() { return active; }
    public void deactivate() { active = false; }

    @Override
    public String toString() {
        return String.format("%s: %s (%d credits) [%s] - %s", code, title, credits, semester, department);
    }

    public static class Builder {
        private final String code;
        private String title = "Untitled";
        private int credits = 3;
        private Instructor instructor;
        private Semester semester = Semester.FALL;
        private String department = "General";

        public Builder(String code) { this.code = Objects.requireNonNull(code); }
        public Builder title(String t) { this.title = t; return this; }
        public Builder credits(int c) { this.credits = c; return this; }
        public Builder instructor(Instructor i) { this.instructor = i; return this; }
        public Builder semester(Semester s) { this.semester = s; return this; }
        public Builder department(String d) { this.department = d; return this; }
        public Course build() { return new Course(this); }
    }
}

// ----------------------------
// File: src/edu/ccrm/domain/Enrollment.java
// ----------------------------
package edu.ccrm.domain;

import java.time.LocalDate;

public class Enrollment {
    private final Student student;
    private final Course course;
    private final LocalDate enrolledOn;
    private Grade grade; // nullable

    public Enrollment(Student student, Course course) {
        this.student = student;
        this.course = course;
        this.enrolledOn = LocalDate.now();
    }

    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public LocalDate getEnrolledOn() { return enrolledOn; }
    public Grade getGrade() { return grade; }
    public void setGrade(Grade g) { this.grade = g; }

    @Override
    public String toString() {
        return String.format("%s | %s | Enrolled: %s | Grade: %s", course.getCode(), course.getTitle(), enrolledOn, grade == null ? "N/A" : grade);
    }
}

// ----------------------------
// File: src/edu/ccrm/config/DataStore.java  (Singleton in-memory store)
// ----------------------------
package edu.ccrm.config;

import edu.ccrm.domain.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Student> students = new ConcurrentHashMap<>();
    private final Map<String, Course> courses = new ConcurrentHashMap<>();
    private final Map<String, Instructor> instructors = new ConcurrentHashMap<>();

    private DataStore() {}

    public static DataStore getInstance() { return INSTANCE; }

    public Map<String, Student> students() { return students; }
    public Map<String, Course> courses() { return courses; }
    public Map<String, Instructor> instructors() { return instructors; }
}

// ----------------------------
// File: src/edu/ccrm/service/StudentService.java
// ----------------------------
package edu.ccrm.service;

import edu.ccrm.domain.Student;
import java.util.List;

public interface StudentService {
    Student addStudent(String id, String regNo, String fullName, String email);
    Student findById(String id);
    List<Student> listAll();
    void deactivate(String id);
}

// ----------------------------
// File: src/edu/ccrm/service/impl/StudentServiceImpl.java
// ----------------------------
package edu.ccrm.service.impl;

import edu.ccrm.service.StudentService;
import edu.ccrm.domain.Student;
import edu.ccrm.config.DataStore;

import java.util.*;
import java.util.stream.Collectors;

public class StudentServiceImpl implements StudentService {
    private final DataStore ds = DataStore.getInstance();

    @Override
    public Student addStudent(String id, String regNo, String fullName, String email) {
        Student s = new Student(id, regNo, fullName, email);
        ds.students().put(id, s);
        return s;
    }

    @Override
    public Student findById(String id) {
        return ds.students().get(id);
    }

    @Override
    public List<Student> listAll() {
        return new ArrayList<>(ds.students().values());
    }

    @Override
    public void deactivate(String id) {
        Student s = ds.students().get(id);
        if (s != null) s.deactivate();
    }
}

// ----------------------------
// File: src/edu/ccrm/service/CourseService.java
// ----------------------------
package edu.ccrm.service;

import edu.ccrm.domain.Course;
import java.util.List;

public interface CourseService {
    Course addCourse(Course course);
    Course findByCode(String code);
    List<Course> listAll();
}

// ----------------------------
// File: src/edu/ccrm/service/impl/CourseServiceImpl.java
// ----------------------------
package edu.ccrm.service.impl;

import edu.ccrm.config.DataStore;
import edu.ccrm.domain.Course;
import edu.ccrm.service.CourseService;

import java.util.*;
import java.util.stream.Collectors;

public class CourseServiceImpl implements CourseService {
    private final DataStore ds = DataStore.getInstance();

    @Override
    public Course addCourse(Course course) {
        ds.courses().put(course.getCode(), course);
        return course;
    }

    @Override
    public Course findByCode(String code) {
        return ds.courses().get(code);
    }

    @Override
    public List<Course> listAll() {
        return new ArrayList<>(ds.courses().values());
    }
}

// ----------------------------
// File: src/edu/ccrm/service/EnrollmentService.java
// ----------------------------
package edu.ccrm.service;

import edu.ccrm.domain.Enrollment;
import edu.ccrm.domain.Grade;

public interface EnrollmentService {
    Enrollment enroll(String studentId, String courseCode) throws Exception; // TODO custom exceptions
    void unenroll(String studentId, String courseCode);
    void recordGrade(String studentId, String courseCode, Grade grade);
}

// ----------------------------
// File: src/edu/ccrm/service/impl/EnrollmentServiceImpl.java
// ----------------------------
package edu.ccrm.service.impl;

import edu.ccrm.config.DataStore;
import edu.ccrm.domain.*;
import edu.ccrm.service.EnrollmentService;

public class EnrollmentServiceImpl implements EnrollmentService {
    private final DataStore ds = DataStore.getInstance();

    @Override
    public Enrollment enroll(String studentId, String courseCode) throws Exception {
        Student s = ds.students().get(studentId);
        Course c = ds.courses().get(courseCode);
        if (s == null || c == null) throw new Exception("Student or Course not found");
        Enrollment e = new Enrollment(s, c);
        s.enroll(e);
        return e;
    }

    @Override
    public void unenroll(String studentId, String courseCode) {
        Student s = ds.students().get(studentId);
        if (s != null) s.unenroll(courseCode);
    }

    @Override
    public void recordGrade(String studentId, String courseCode, Grade grade) {
        Student s = ds.students().get(studentId);
        if (s == null) return;
        Enrollment e = s.getEnrollments().stream().filter(en -> en.getCourse().getCode().equals(courseCode)).findFirst().orElse(null);
        if (e != null) e.setGrade(grade);
    }
}

// ----------------------------
// File: src/edu/ccrm/io/ImportExportService.java
// ----------------------------
package edu.ccrm.io;

import edu.ccrm.config.DataStore;
import edu.ccrm.domain.Course;
import edu.ccrm.domain.Student;
import edu.ccrm.domain.Course;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class ImportExportService {
    private final DataStore ds = DataStore.getInstance();

    public void exportStudents(Path out) throws IOException {
        Files.createDirectories(out.getParent());
        List<String> lines = ds.students().values().stream()
                .map(s -> String.join(",", s.getId(), s.getRegNo(), s.getFullName(), s.getEmail()))
                .collect(Collectors.toList());
        Files.write(out, lines, StandardCharsets.UTF_8);
    }

    public void importStudents(Path in) throws IOException {
        List<String> lines = Files.readAllLines(in, StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] cols = line.split(",");
            if (cols.length >= 4) {
                Student s = new Student(cols[0], cols[1], cols[2], cols[3]);
                ds.students().put(s.getId(), s);
            }
        }
    }

    // TODO export/import courses, enrollments. Use Files.walk for recursive utilities and backups.
}

// ----------------------------
// File: src/edu/ccrm/util/RecursionUtil.java
// ----------------------------
package edu.ccrm.util;

import java.io.IOException;
import java.nio.file.*;

public class RecursionUtil {
    // recursively compute total size of a directory
    public static long directorySize(Path p) throws IOException {
        final long[] total = {0L};
        Files.walk(p).forEach(path -> {
            try {
                if (Files.isRegularFile(path)) total[0] += Files.size(path);
            } catch (IOException e) {
                // ignore
            }
        });
        return total[0];
    }
}

// ----------------------------
// File: src/edu/ccrm/cli/CCRMApp.java  (main CLI)
// ----------------------------
package edu.ccrm.cli;

import edu.ccrm.config.AppConfig;
import edu.ccrm.service.*;
import edu.ccrm.service.impl.*;
import edu.ccrm.domain.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

public class CCRMApp {
    private static final StudentService studentService = new StudentServiceImpl();
    private static final CourseService courseService = new CourseServiceImpl();
    private static final EnrollmentService enrollmentService = new EnrollmentServiceImpl();

    public static void main(String[] args) {
        AppConfig cfg = AppConfig.getInstance();
        System.out.println("CCRM starting... data folder: " + cfg.getDataFolder());

        // simple seed
        seedSampleData();

        Scanner sc = new Scanner(System.in);
        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> manageStudents(sc);
                case "2" -> manageCourses(sc);
                case "3" -> manageEnrollment(sc);
                case "4" -> {
                    System.out.println("Export/Backup not implemented in this scaffold.");
                }
                case "0" -> { running = false; }
                default -> System.out.println("Unknown option");
            }
        }
        System.out.println("Exiting CCRM. Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n=== CCRM Menu ===");
        System.out.println("1) Manage Students");
        System.out.println("2) Manage Courses");
        System.out.println("3) Enrollment & Grades");
        System.out.println("4) Import/Export/Backup");
        System.out.println("0) Exit");
        System.out.print("Choose: ");
    }

    private static void manageStudents(Scanner sc) {
        System.out.println("Student Menu: 1=Add,2=List,3=Transcript,4=Deactivate,0=Back");
        String ch = sc.nextLine();
        switch (ch) {
            case "1":
                System.out.print("Full name: "); String name = sc.nextLine();
                System.out.print("Email: "); String email = sc.nextLine();
                String id = UUID.randomUUID().toString();
                String regNo = "R" + (int)(Math.random()*10000);
                studentService.addStudent(id, regNo, name, email);
                System.out.println("Added: " + name);
                break;
            case "2":
                List<Student> list = studentService.listAll();
                list.forEach(s -> System.out.println(s.getProfile()));
                break;
            case "3":
                System.out.print("Student id: "); String sid = sc.nextLine();
                Student s = (Student) studentService.findById(sid);
                if (s == null) System.out.println("Not found"); else System.out.println(s.getTranscript());
                break;
            case "4":
                System.out.print("Student id: "); String did = sc.nextLine();
                studentService.deactivate(did);
                System.out.println("Deactivated if exists.");
                break;
            default:
                break;
        }
    }

    private static void manageCourses(Scanner sc) {
        System.out.println("Course Menu: 1=Add,2=List,0=Back");
        String ch = sc.nextLine();
        switch (ch) {
            case "1":
                System.out.print("Course code: "); String code = sc.nextLine();
                System.out.print("Title: "); String title = sc.nextLine();
                System.out.print("Credits: "); int credits = Integer.parseInt(sc.nextLine());
                Course c = new Course.Builder(code).title(title).credits(credits).build();
                courseService.addCourse(c);
                System.out.println("Added: " + c);
                break;
            case "2":
                courseService.listAll().forEach(System.out::println);
                break;
            default:
                break;
        }
    }

    private static void manageEnrollment(Scanner sc) {
        System.out.println("Enrollment Menu: 1=Enroll,2=Unenroll,3=RecordGrade,0=Back");
        String ch = sc.nextLine();
        switch (ch) {
            case "1":
                System.out.print("Student id: "); String sid = sc.nextLine();
                System.out.print("Course code: "); String cc = sc.nextLine();
                try { enrollmentService.enroll(sid, cc); System.out.println("Enrolled."); } catch (Exception e) { System.out.println("Failed: " + e.getMessage()); }
                break;
            case "2":
                System.out.print("Student id: "); String sid2 = sc.nextLine();
                System.out.print("Course code: "); String cc2 = sc.nextLine();
                enrollmentService.unenroll(sid2, cc2);
                System.out.println("Unenrolled (if existed).");
                break;
            case "3":
                System.out.print("Student id: "); String sid3 = sc.nextLine();
                System.out.print("Course code: "); String cc3 = sc.nextLine();
                System.out.print("Grade (S/A/B/C/D/E/F): "); String g = sc.nextLine();
                try { Grade grade = Grade.valueOf(g.toUpperCase()); enrollmentService.recordGrade(sid3, cc3, grade); System.out.println("Recorded."); } catch (IllegalArgumentException ex) { System.out.println("Invalid grade"); }
                break;
            default:
                break;
        }
    }

    private static void seedSampleData() {
        // seed one instructor, two courses, one student
        Instructor i = new Instructor("ins-1", "Dr. Alice", "alice@uni.edu", "CSE");
        Course c1 = new Course.Builder("CSE101").title("Intro to CS").credits(4).instructor(i).department("CSE").semester(Semester.FALL).build();
        Course c2 = new Course.Builder("MAT101").title("Calculus").credits(3).department("Math").semester(Semester.FALL).build();
        courseService.addCourse(c1); courseService.addCourse(c2);
        Student s = studentService.addStudent("stu-1", "R1001", "Bob Student", "bob@uni.edu");
    }
}

/*
Notes and next steps (to implement):
 - Implement custom exceptions: DuplicateEnrollmentException, MaxCreditLimitExceededException
 - Implement file backup using Files.copy and timestamped folder from AppConfig
 - Build CLI pagination and input validation (Validators util class)
 - Add tests (unit tests) and sample CSV data under resources/
 - Add README screenshots and step-by-step install instructions
 - Use assertions for invariants and document how to run with -ea
 - Add nested classes / anonymous inner class example (e.g., InputListener)
 - Add more advanced Stream pipelines for reports (GPA distribution, top students)
*/
