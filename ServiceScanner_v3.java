/*
 * 잘 출력됨. csv 출력위해 v4
──────────────────────────────
파일명: Service1PSC.java
ServiceId : service1
ServiceName : 서비스1
INDTO : ServiceInputDTO
OUTDTO : ServiceOutputDTO
 */
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
import java.nio.charset.*;

public class ServiceScanner_v3 {
    public static void main(String[] args) throws IOException {
        Path folder = Paths.get(args[0]);
        System.out.println("▶ 처리하는 폴더: " + folder);

        Files.walk(folder)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(file -> {
                try {
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    processFile(file, content);
                } catch(IOException e) {
                    System.err.println("파일 읽기 실패: " + file);
                }
            });
    }
    
    private static void processFile(Path file, String content) {
        // @ServiceId와 @ServiceName 어노테이션 찾기
        String serviceId = null;
        String serviceName = null;
        
        Matcher idMatcher = Pattern.compile("@ServiceId\\(([^)]*)\\)").matcher(content);
        if (idMatcher.find()) {
            String ann = idMatcher.group(1);
            serviceId = !ann.contains("=") ? findQuoted(ann) : extract(ann, "value");
        }
        
        Matcher nameMatcher = Pattern.compile("@ServiceName\\(([^)]*)\\)").matcher(content);
        if (nameMatcher.find()) {
            String ann = nameMatcher.group(1);
            serviceName = !ann.contains("=") ? findQuoted(ann) : extract(ann, "value");
        }
        
        // 어노테이션을 찾았을 때만 메서드 시그니처와 DTO 정보 추출
        if (serviceId != null || serviceName != null) {
            // 메서드 시그니처 추출
            Matcher methodMatcher = Pattern.compile("public\\s+(\\w+)\\s+(\\w+)\\((\\w+)\\s+\\w+\\)").matcher(content);
            if (methodMatcher.find()) {
                String outDto = methodMatcher.group(1);
                String inDto = methodMatcher.group(3);
                
                System.out.println("\n──────────────────────────────");
                System.out.println("파일명: " + file.getFileName());
                System.out.println("ServiceId : " + (serviceId != null ? serviceId : "(없음)"));
                System.out.println("ServiceName : " + (serviceName != null ? serviceName : "(없음)"));
                System.out.println("INDTO : " + inDto);
                System.out.println("OUTDTO : " + outDto);
            }
        }
    }

    private static String findQuoted(String text) {
        Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static String extract(String input, String key) {
        Matcher m = Pattern.compile(key + "\\s*=\\s*\"([^\"]+)\"").matcher(input);
        return m.find() ? m.group(1) : null;
    }
}