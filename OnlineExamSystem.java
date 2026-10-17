import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Online Exam System (console application).
 *
 * Teachers create timed multiple-choice exams; students log in, sit an exam against the clock,
 * get it marked automatically and can review their results history.
 *
 * Run:  javac OnlineExamSystem.java && java OnlineExamSystem
 * Demo accounts: teacher1 / teacher123, student1 / student123
 */
public class OnlineExamSystem {

    // ------------------------------------------------------------------ model

    enum Role { TEACHER, STUDENT }

    static final class User {
        private final String username;
        private final String passwordHash;
        private final Role role;

        User(String username, String passwordHash, Role role) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.role = role;
        }

        String getUsername() { return username; }
        String getPasswordHash() { return passwordHash; }
        Role getRole() { return role; }
    }

    static final class Question {
        private final String content;
        private final List<String> options;
        private final int correctOption; // 0-based index into options

        Question(String content, List<String> options, int correctOption) {
            if (options.size() < 2) throw new IllegalArgumentException("A question needs at least two options");
            if (correctOption < 0 || correctOption >= options.size())
                throw new IllegalArgumentException("Correct option is out of range");
            this.content = content;
            this.options = List.copyOf(options);
            this.correctOption = correctOption;
        }

        String getContent() { return content; }
        List<String> getOptions() { return options; }
        int getCorrectOption() { return correctOption; }
    }

    static final class Exam {
        private final int id;
        private final String title;
        private final int durationMinutes;
        private final List<Question> questions;
        private final String createdBy;

        Exam(int id, String title, int durationMinutes, List<Question> questions, String createdBy) {
            if (questions.isEmpty()) throw new IllegalArgumentException("An exam needs at least one question");
            this.id = id;
            this.title = title;
            this.durationMinutes = durationMinutes;
            this.questions = List.copyOf(questions);
            this.createdBy = createdBy;
        }

        int getId() { return id; }
        String getTitle() { return title; }
        int getDurationMinutes() { return durationMinutes; }
        List<Question> getQuestions() { return questions; }
        String getCreatedBy() { return createdBy; }
    }

    static final class ExamResult {
        private final String student;
        private final String examTitle;
        private final int correct;
        private final int total;
        private final boolean timedOut;
        private final LocalDateTime takenAt;

        ExamResult(String student, String examTitle, int correct, int total, boolean timedOut) {
            this.student = student;
            this.examTitle = examTitle;
            this.correct = correct;
            this.total = total;
            this.timedOut = timedOut;
            this.takenAt = LocalDateTime.now();
        }

        int getScorePercent() { return (int) Math.round(100.0 * correct / total); }
        boolean isPass() { return getScorePercent() >= PASS_MARK; }

        @Override
        public String toString() {
            return String.format("%-22s %3d%%  (%d/%d)  %-4s%s  %s",
                    examTitle, getScorePercent(), correct, total, isPass() ? "PASS" : "FAIL",
                    timedOut ? "  [time ran out]" : "", takenAt.format(TIME_FORMAT));
        }
    }

    // ------------------------------------------------------------------ state

    static final int PASS_MARK = 70;
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final Map<String, User> users = new LinkedHashMap<>();
    private final List<Exam> exams = new ArrayList<>();
    private final Map<String, List<ExamResult>> resultsByStudent = new HashMap<>();
    private int nextExamId = 1;

    // ------------------------------------------------------------------ core logic

    boolean registerUser(String username, String password, Role role) {
        if (username.isBlank() || password.length() < 6 || users.containsKey(username)) return false;
        users.put(username, new User(username, hash(password), role));
        return true;
    }

    Optional<User> authenticate(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPasswordHash().equals(hash(password))) return Optional.of(user);
        return Optional.empty();
    }

    Exam createExam(User teacher, String title, int durationMinutes, List<Question> questions) {
        if (teacher.getRole() != Role.TEACHER) throw new IllegalStateException("Only teachers can create exams");
        Exam exam = new Exam(nextExamId++, title, durationMinutes, questions, teacher.getUsername());
        exams.add(exam);
        return exam;
    }

    /** Marks a submission. Unanswered questions (null) count as incorrect. */
    ExamResult submit(User student, Exam exam, List<Integer> answers, boolean timedOut) {
        if (student.getRole() != Role.STUDENT) throw new IllegalStateException("Only students can sit exams");
        int correct = 0;
        for (int i = 0; i < exam.getQuestions().size(); i++) {
            Integer answer = i < answers.size() ? answers.get(i) : null;
            if (answer != null && answer == exam.getQuestions().get(i).getCorrectOption()) correct++;
        }
        ExamResult result = new ExamResult(student.getUsername(), exam.getTitle(), correct,
                exam.getQuestions().size(), timedOut);
        resultsByStudent.computeIfAbsent(student.getUsername(), k -> new ArrayList<>()).add(result);
        return result;
    }

    List<ExamResult> historyFor(String student) {
        return resultsByStudent.getOrDefault(student, List.of());
    }

    List<ExamResult> allResults() {
        List<ExamResult> all = new ArrayList<>();
        resultsByStudent.values().forEach(all::addAll);
        all.sort(Comparator.comparing((ExamResult r) -> r.takenAt));
        return all;
    }

    static String hash(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ console UI

    private final Scanner in;

    OnlineExamSystem(Scanner in) {
        this.in = in;
    }

    void seedDemoData() {
        registerUser("teacher1", "teacher123", Role.TEACHER);
        registerUser("student1", "student123", Role.STUDENT);
        User teacher = users.get("teacher1");
        createExam(teacher, "General Knowledge", 5, List.of(
                new Question("What is the capital of France?", List.of("Berlin", "Paris", "Rome"), 1),
                new Question("What is 2 + 2?", List.of("3", "4", "5"), 1),
                new Question("Is the Earth round?", List.of("True", "False"), 0)));
        createExam(teacher, "Java Basics", 10, List.of(
                new Question("Which keyword creates an object?", List.of("class", "new", "this", "static"), 1),
                new Question("Which collection keeps keys unique?", List.of("List", "Map", "Queue"), 1),
                new Question("What does 'final' on a variable mean?",
                        List.of("It can't be reassigned", "It is private", "It is static"), 0)));
    }

    void run() {
        System.out.println("=== Online Exam System ===");
        System.out.println("Demo logins: teacher1/teacher123, student1/student123");
        while (true) {
            System.out.println("\n1) Log in   2) Register   0) Exit");
            switch (readLine("> ")) {
                case "1" -> login();
                case "2" -> register();
                case "0" -> { System.out.println("Goodbye."); return; }
                default -> System.out.println("Please choose 1, 2 or 0.");
            }
        }
    }

    private void register() {
        String username = readLine("Username: ").trim();
        String password = readLine("Password (min 6 characters): ");
        Role role = readLine("Role - (t)eacher or (s)tudent: ").trim().toLowerCase().startsWith("t")
                ? Role.TEACHER : Role.STUDENT;
        if (registerUser(username, password, role)) System.out.println("Registered " + username + " as " + role + ".");
        else System.out.println("Registration failed: username taken, blank, or password too short.");
    }

    private void login() {
        Optional<User> user = authenticate(readLine("Username: ").trim(), readLine("Password: "));
        if (user.isEmpty()) { System.out.println("Invalid username or password."); return; }
        System.out.println("Welcome, " + user.get().getUsername() + ".");
        if (user.get().getRole() == Role.TEACHER) teacherMenu(user.get()); else studentMenu(user.get());
    }

    private void teacherMenu(User teacher) {
        while (true) {
            System.out.println("\n[Teacher] 1) Create exam  2) List exams  3) View all results  0) Log out");
            switch (readLine("> ")) {
                case "1" -> createExamInteractive(teacher);
                case "2" -> listExams();
                case "3" -> {
                    List<ExamResult> all = allResults();
                    if (all.isEmpty()) System.out.println("No results yet.");
                    all.forEach(r -> System.out.println(String.format("%-12s ", r.student) + r));
                }
                case "0" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void studentMenu(User student) {
        while (true) {
            System.out.println("\n[Student] 1) Take an exam  2) My results  0) Log out");
            switch (readLine("> ")) {
                case "1" -> takeExamInteractive(student);
                case "2" -> {
                    List<ExamResult> mine = historyFor(student.getUsername());
                    if (mine.isEmpty()) System.out.println("You haven't taken any exams yet.");
                    mine.forEach(System.out::println);
                }
                case "0" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void createExamInteractive(User teacher) {
        String title = readLine("Exam title: ").trim();
        int minutes = readInt("Time limit in minutes: ", 1, 300);
        int count = readInt("Number of questions: ", 1, 100);
        List<Question> questions = new ArrayList<>();
        for (int q = 1; q <= count; q++) {
            String text = readLine("Question " + q + ": ");
            int optionCount = readInt("  How many options? ", 2, 6);
            List<String> options = new ArrayList<>();
            for (int o = 1; o <= optionCount; o++) options.add(readLine("  Option " + o + ": "));
            int correct = readInt("  Number of the correct option: ", 1, optionCount) - 1;
            questions.add(new Question(text, options, correct));
        }
        Exam exam = createExam(teacher, title, minutes, questions);
        System.out.println("Created exam #" + exam.getId() + " \"" + exam.getTitle() + "\" with " + count + " questions.");
    }

    private void listExams() {
        if (exams.isEmpty()) { System.out.println("No exams yet."); return; }
        for (Exam e : exams)
            System.out.printf("#%d  %-22s %2d questions  %3d min  (by %s)%n",
                    e.getId(), e.getTitle(), e.getQuestions().size(), e.getDurationMinutes(), e.getCreatedBy());
    }

    private void takeExamInteractive(User student) {
        listExams();
        if (exams.isEmpty()) return;
        int id = readInt("Exam number: ", 1, Integer.MAX_VALUE);
        Optional<Exam> found = exams.stream().filter(e -> e.getId() == id).findFirst();
        if (found.isEmpty()) { System.out.println("No exam with that number."); return; }
        Exam exam = found.get();

        Instant deadline = Instant.now().plus(Duration.ofMinutes(exam.getDurationMinutes()));
        System.out.println("\n" + exam.getTitle() + " - " + exam.getQuestions().size() + " questions, "
                + exam.getDurationMinutes() + " minute limit. Answers after the deadline are not counted.");

        List<Integer> answers = new ArrayList<>();
        boolean timedOut = false;
        for (int i = 0; i < exam.getQuestions().size(); i++) {
            long secondsLeft = Duration.between(Instant.now(), deadline).getSeconds();
            if (secondsLeft <= 0) { timedOut = true; break; }
            Question q = exam.getQuestions().get(i);
            System.out.printf("%nQ%d. %s   (%d:%02d left)%n", i + 1, q.getContent(), secondsLeft / 60, secondsLeft % 60);
            for (int o = 0; o < q.getOptions().size(); o++) System.out.println("  " + (o + 1) + ") " + q.getOptions().get(o));
            int choice = readInt("Your answer: ", 1, q.getOptions().size()) - 1;
            if (Instant.now().isAfter(deadline)) { timedOut = true; break; }
            answers.add(choice);
        }

        ExamResult result = submit(student, exam, answers, timedOut);
        System.out.println("\n" + (timedOut ? "Time is up. " : "") + "Submitted.");
        System.out.println("Score: " + result.getScorePercent() + "% (" + result.correct + "/" + result.total + ") - "
                + (result.isPass() ? "PASS" : "FAIL") + " (pass mark " + PASS_MARK + "%)");
        reviewAnswers(exam, answers);
    }

    private void reviewAnswers(Exam exam, List<Integer> answers) {
        System.out.println("Review:");
        for (int i = 0; i < exam.getQuestions().size(); i++) {
            Question q = exam.getQuestions().get(i);
            Integer given = i < answers.size() ? answers.get(i) : null;
            boolean ok = given != null && given == q.getCorrectOption();
            System.out.printf("  Q%d %s  correct: %s%s%n", i + 1, ok ? "[ok]" : "[x] ",
                    q.getOptions().get(q.getCorrectOption()),
                    given == null ? "  (not answered)" : ok ? "" : "  (you chose " + q.getOptions().get(given) + ")");
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) { System.out.println(); System.exit(0); }
        return in.nextLine();
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            String raw = readLine(prompt).trim();
            try {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    public static void main(String[] args) {
        OnlineExamSystem app = new OnlineExamSystem(new Scanner(System.in));
        app.seedDemoData();
        app.run();
    }
}
