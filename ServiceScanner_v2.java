/*
 * ServiceId("svc1")
 * ServiceId(value="svc1")
 * 이걸 못찾아서 수정하여 v3
 */
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.*;
import java.nio.charset.StandardCharsets;

public class ServiceScanner_v2 {
    public static void main(String[] args) throws IOException {
        Path folder = Paths.get(args[0]);
        System.out.println("▶ 처리하는 폴더: " + folder);

        Files.walk(folder)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(file -> {
                try {
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    Matcher m = Pattern.compile("@ServiceId\\(([^)]*)\\)").matcher(content);
                    while (m.find()) {
                        String annotation = m.group(1);
                        String serviceId = extract(annotation, "value");
                        String serviceName = extract(annotation, "serviceName");

                        System.out.println("\n──────────────────────────────");
                        System.out.println("파일명: " + file.getFileName());
                        System.out.println("  serviceId: " + (serviceId != null ? serviceId : "(없음)"));
                        System.out.println("  serviceName: " + (serviceName != null ? serviceName : "(없음)"));

                        // @ServiceId 뒤의 public 메서드 선언 추출
                        int start = m.end();
                        Matcher methodMatcher = Pattern.compile("public\\s+[^{]+\\{").matcher(content.substring(start));
                        if (methodMatcher.find()) {
                            String methodLine = methodMatcher.group().trim();
                            System.out.println("  메서드 시그니처: " + methodLine);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("파일 읽기 실패: " + file);
                }
            });
    }

    private static String extract(String input, String key) {
        Matcher m = Pattern.compile(key + "\\s*=\\s*\"([^\"]+)\"").matcher(input);
        return m.find() ? m.group(1) : null;
    }
}
