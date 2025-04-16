/*
 * java8 이상
 * 컴파일 : javac -encoding UTF-8 ServiceScanner_v5.java
 * 실행 : java ServiceScanner_v5 ./src/main/java/hlicp/ics/psi > k.csv
 */
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
import java.nio.charset.*;
import java.util.concurrent.*;

public class ServiceScanner_v5 {
    private static final Pattern SVC_ID_PATTERN = Pattern.compile("@ServiceId\\(([^)]*)\\)");
    private static final Pattern SVC_NAME_PATTERN = Pattern.compile("@ServiceName\\(([^)]*)\\)");
    private static final Pattern METHOD_PATTERN = Pattern.compile("public\\s+([\\w\\.]+)\\s+\\w+\\s*\\(\\s*([\\w\\.]+)\\s+\\w+\\s*\\)", Pattern.DOTALL);
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) { System.err.println("사용법: java ServiceScanner_v2 <폴더경로>"); System.exit(1); }
        Path folder = Paths.get(args[0]);
        System.out.println("파일경로,파일명,ServiceId,ServiceName,INDTO,OUTDTO");
        
        ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Files.walk(folder).filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java"))
            .map(file -> exec.submit(() -> processFile(file)))
            .toArray(Future[]::new);
        
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.HOURS);
    }
    
    private static void processFile(Path file) {
        try {
            String[] lines = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).split("\\r?\\n");
            
            for (int i = 0; i < lines.length; i++) {
                if (SVC_ID_PATTERN.matcher(lines[i]).find()) {
                    StringBuilder block = new StringBuilder(lines[i]);
                    for (int j = 1; j <= 8 && i + j < lines.length; j++) block.append("\n").append(lines[i + j]);
                    String blockText = block.toString();
                    
                    String svcId = extractValue(blockText, SVC_ID_PATTERN);
                    String svcName = extractValue(blockText, SVC_NAME_PATTERN);
                    String[] dtos = extractDTOs(blockText);
                    
                    if (svcId != null && dtos != null) {
                        String path = file.getParent() != null ? file.getParent().toString() : "";
                        String fileName = file.getFileName().toString();
                        synchronized(System.out) {
                            System.out.println(csvEscape(path) + "," + csvEscape(fileName) + "," + 
                                            csvEscape(svcId) + "," + csvEscape(svcName != null ? svcName : "") + "," + 
                                            dtos[0] + "," + dtos[1]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("파일 읽기 실패: " + file);
        }
    }
    
    private static String extractValue(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return null;
        String content = m.group(1);
        if (content.contains("value")) {
            Matcher vm = Pattern.compile("value\\s*=\\s*\"([^\"]+)\"").matcher(content);
            return vm.find() ? vm.group(1) : null;
        }
        Matcher qm = Pattern.compile("\"([^\"]+)\"").matcher(content);
        return qm.find() ? qm.group(1) : null;
    }
    
    private static String[] extractDTOs(String text) {
        Matcher m = METHOD_PATTERN.matcher(text);
        return m.find() ? new String[] { m.group(2), m.group(1) } : null;
    }
    
    private static String csvEscape(String val) {
        if (val == null) return "";
        return val.contains(",") || val.contains("\"") || val.contains("\n") ? 
               "\"" + val.replace("\"", "\"\"") + "\"" : val;
    }
}