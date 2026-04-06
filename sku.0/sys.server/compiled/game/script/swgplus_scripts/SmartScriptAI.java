package script.swgplus_scripts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility entry point that inspects the {@code script.swgplus_scripts} package for
 * common issues and modernization opportunities.
 * <p>
 * The legacy version of this tool attempted to mutate source files on disk, which was risky
 * and offered little feedback on what actually needed attention. This rewrite focuses on
 * producing actionable diagnostics without touching the source files. The resulting Markdown
 * report can be reviewed by developers and used as a modernization checklist.
 */
public final class SmartScriptAI {

    private static final Path DEFAULT_ROOT = Paths.get(
        "sku.0",
        "sys.server",
        "compiled",
        "game",
        "script",
        "swgplus_scripts"
    );

    private static final Path REPORT_DIRECTORY = Paths.get("reports", "swgplus_scripts");
    private static final int MAX_LINE_LENGTH = 160;

    private SmartScriptAI() {
    }

    public static void main(String[] args) {
        Path root = args.length > 0 ? Paths.get(args[0]) : DEFAULT_ROOT;
        Instant start = Instant.now();

        try {
            AnalysisReport report = analyse(root);
            writeMarkdownReport(report);
            Duration elapsed = Duration.between(start, Instant.now());
            long totalMillis = elapsed.toMillis();
            long seconds = totalMillis / 1000;
            long millis = totalMillis % 1000;
            System.out.printf(
                "Analysed %d script(s) in %d.%03d seconds.%n",
                report.getFileReports().size(),
                seconds,
                millis
            );
            report.printSummary();
        } catch (IOException ioException) {
            System.err.println("Failed to analyse scripts under " + root + ": " + ioException.getMessage());
            ioException.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static AnalysisReport analyse(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("Root path " + root + " is not a directory");
        }

        List<FileReport> fileReports = new ArrayList<>();

        try (Stream<Path> pathStream = Files.walk(root)) {
            pathStream
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))
                .sorted()
                .forEach(path -> fileReports.add(inspectFile(path)));
        }

        return new AnalysisReport(root, fileReports);
    }

    private static FileReport inspectFile(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            return FileReport.failure(path, "Unable to read file: " + ioException.getMessage());
        }

        FileReport report = FileReport.success(path, countNonEmptyLines(lines));
        boolean containsPackage = lines.stream()
            .anyMatch(line -> line.trim().startsWith("package script.swgplus_scripts"));
        if (!containsPackage) {
            report.addProblem("Missing or incorrect package declaration (expected script.swgplus_scripts).");
        }

        if (lines.stream().anyMatch(line -> line.contains("System.out.print") || line.contains("System.out.println"))) {
            report.addProblem("Uses System.out for logging; prefer server logging helpers.");
        }

        boolean extendsBaseScript = lines.stream()
            .anyMatch(line -> line.contains("extends script.base_script"));
        boolean declaresEventHandlers = lines.stream()
            .map(String::trim)
            .anyMatch(line -> line.startsWith("public int On"));
        boolean hasOverrideAnnotations = lines.stream()
            .map(String::trim)
            .anyMatch(line -> line.startsWith("@Override"));

        if (extendsBaseScript && declaresEventHandlers && !hasOverrideAnnotations) {
            report.addSuggestion("Consider adding @Override to event handlers for clarity.");
        }

        detectMessageToIssues(lines, report);
        detectTestingMessages(lines, report);
        detectTrailingWhitespace(lines, report);
        detectLongLines(lines, report);
        detectTodoMarkers(lines, report);

        return report;
    }

    private static void detectMessageToIssues(List<String> lines, FileReport report) {
        for (int index = 0; index < lines.size(); index++) {
            String trimmed = lines.get(index).trim();
            boolean zeroDelay = trimmed.endsWith(", 0, false);")
                || trimmed.endsWith(", 0f, false);")
                || trimmed.endsWith(", 0.0f, false);")
                || trimmed.endsWith(", 0.0F, false);");
            if (trimmed.startsWith("messageTo(") && zeroDelay) {
                report.addProblem(index + 1, "messageTo invoked with zero delay; confirm this will not create tight server loops.");
            }
        }
    }

    private static void detectTestingMessages(List<String> lines, FileReport report) {
        long testMessages = lines.stream()
            .filter(line -> line.contains("sendSystemMessageTestingOnly") || line.contains("sendSystemMessageGalaxyTestingOnly"))
            .count();
        if (testMessages > 0) {
            report.addSuggestion(
                "Found " + testMessages + " testing-only system message call(s); evaluate whether production-safe messaging should be used."
            );
        }
    }

    private static void detectTrailingWhitespace(List<String> lines, FileReport report) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (!line.equals(line.stripTrailing())) {
                report.addProblem(index + 1, "Trailing whitespace detected.");
            }
        }
    }

    private static void detectLongLines(List<String> lines, FileReport report) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.length() > MAX_LINE_LENGTH) {
                report.addSuggestion(index + 1, "Line length exceeds " + MAX_LINE_LENGTH + " characters.");
            }
        }
    }

    private static void detectTodoMarkers(List<String> lines, FileReport report) {
        for (int index = 0; index < lines.size(); index++) {
            String trimmed = lines.get(index).trim();
            if (trimmed.contains("TODO") || trimmed.contains("FIXME")) {
                report.addSuggestion(index + 1, "Contains TODO/FIXME marker: " + trimmed);
            }
        }
    }

    private static long countNonEmptyLines(List<String> lines) {
        return lines.stream().map(String::trim).filter(line -> !line.isEmpty()).count();
    }

    private static void writeMarkdownReport(AnalysisReport report) throws IOException {
        Files.createDirectories(REPORT_DIRECTORY);
        Path reportFile = REPORT_DIRECTORY.resolve("swgplus_scripts_report.md");
        List<String> lines = new ArrayList<>();
        lines.add("# SWG+ Script Package Analysis");
        lines.add("");
        lines.add("Root directory: ``" + report.getRoot().toString().replace('\\', '/') + "``");
        lines.add(String.format("Analysed %d script(s).", report.getFileReports().size()));
        lines.add("");

        for (FileReport fileReport : report.getFileReports()) {
            lines.add("## ``" + report.getRoot().relativize(fileReport.getPath()).toString().replace('\\', '/') + "``");
            lines.add("* Lines of code: " + fileReport.getLinesOfCode());
            if (!fileReport.getProblems().isEmpty()) {
                lines.add("* Problems:");
                fileReport.getProblems().forEach(problem -> lines.add("  * " + problem));
            }
            if (!fileReport.getSuggestions().isEmpty()) {
                lines.add("* Suggestions:");
                fileReport.getSuggestions().forEach(suggestion -> lines.add("  * " + suggestion));
            }
            if (fileReport.getProblems().isEmpty() && fileReport.getSuggestions().isEmpty()) {
                lines.add("* No issues detected.");
            }
            lines.add("");
        }

        Files.write(reportFile, lines, StandardCharsets.UTF_8);
    }

    private static final class AnalysisReport {
        private final Path root;
        private final List<FileReport> fileReports;

        private AnalysisReport(Path root, List<FileReport> fileReports) {
            this.root = root;
            this.fileReports = Collections.unmodifiableList(new ArrayList<>(fileReports));
        }

        private Path getRoot() {
            return root;
        }

        private List<FileReport> getFileReports() {
            return fileReports;
        }

        private void printSummary() {
            long totalProblems = fileReports.stream().mapToLong(report -> report.getProblems().size()).sum();
            long totalSuggestions = fileReports.stream().mapToLong(report -> report.getSuggestions().size()).sum();
            System.out.printf("Problems flagged: %d | Suggestions recorded: %d%n", totalProblems, totalSuggestions);

            List<FileReport> highlighted = fileReports.stream()
                .filter(report -> !report.getProblems().isEmpty())
                .sorted((left, right) -> Integer.compare(right.getProblems().size(), left.getProblems().size()))
                .collect(Collectors.toList());

            if (!highlighted.isEmpty()) {
                System.out.println("Scripts needing attention:");
                highlighted.forEach(report -> System.out.printf(
                    "  - %s (%d problems)%n",
                    root.relativize(report.getPath()),
                    report.getProblems().size()
                ));
            }
        }
    }

    private static final class FileReport {
        private final Path path;
        private final List<String> problems = new ArrayList<>();
        private final List<String> suggestions = new ArrayList<>();
        private final boolean success;
        private final String failureMessage;
        private final long linesOfCode;

        private FileReport(Path path, boolean success, String failureMessage, long linesOfCode) {
            this.path = path;
            this.success = success;
            this.failureMessage = failureMessage;
            this.linesOfCode = linesOfCode;
        }

        private static FileReport success(Path path, long linesOfCode) {
            return new FileReport(path, true, null, linesOfCode);
        }

        private static FileReport failure(Path path, String failureMessage) {
            FileReport report = new FileReport(path, false, failureMessage, 0);
            report.addProblem(failureMessage);
            return report;
        }

        private Path getPath() {
            return path;
        }

        private long getLinesOfCode() {
            return linesOfCode;
        }

        private List<String> getProblems() {
            return Collections.unmodifiableList(problems);
        }

        private List<String> getSuggestions() {
            return Collections.unmodifiableList(suggestions);
        }

        private void addProblem(String message) {
            problems.add(Objects.requireNonNull(message));
        }

        private void addProblem(int line, String message) {
            problems.add("Line " + line + ": " + Objects.requireNonNull(message));
        }

        private void addSuggestion(String message) {
            suggestions.add(Objects.requireNonNull(message));
        }

        private void addSuggestion(int line, String message) {
            suggestions.add("Line " + line + ": " + Objects.requireNonNull(message));
        }

        private boolean isSuccess() {
            return success;
        }

        @SuppressWarnings("unused")
        private String getFailureMessage() {
            return failureMessage;
        }
    }
}

