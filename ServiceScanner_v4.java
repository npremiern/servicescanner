/*
 *  잘 나옴.
 */
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceScanner_v4 {
    private static final Pattern SERVICE_ID_PATTERN = Pattern.compile("@ServiceId\\(([^)]*)\\)");
    private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("@ServiceName\\(([^)]*)\\)");
    // 패키지 경로가 포함된 DTO 타입도 추출 가능하도록 패턴 수정
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "public\\s+([\\w\\.]+)\\s+\\w+\\s*\\(\\s*([\\w\\.]+)\\s+\\w+\\s*\\)",
        Pattern.DOTALL
    );
    
    // 파일별 ServiceId 개수를 저장하는 맵
    private static final Map<String, AtomicInteger> fileServiceCount = new ConcurrentHashMap<>();
    
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        if (args.length == 0) {
            System.err.println("사용법: java ServiceScanner_v2 <폴더경로>");
            System.exit(1);
        }
        
        Path folder = Paths.get(args[0]);
        System.out.println("파일경로,파일명,ServiceId,ServiceName,INDTO,OUTDTO");
        
        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        
        Future<?>[] futures = Files.walk(folder)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .map(file -> executor.submit(() -> processFile(file)))
            .toArray(Future[]::new);
        
        for (Future<?> future : futures) {
            future.get();
        }
        
        executor.shutdown();
        
        // 처리 완료 후 파일별 ServiceId 개수 출력
        System.err.println("\n==== 파일별 ServiceId 개수 ====");
        fileServiceCount.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().get() - e1.getValue().get())  // 개수 내림차순 정렬
            .forEach(entry -> System.err.println(entry.getKey() + ": " + entry.getValue() + "개"));
        System.err.println("==== 총 " + fileServiceCount.size() + "개 파일에서 ServiceId 발견 ====");
    }
    
    private static void processFile(Path file) {
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            String[] lines = content.split("\\r?\\n");
            
            AtomicInteger counter = new AtomicInteger(0);
            
            for (int i = 0; i < lines.length; i++) {
                // @ServiceId 찾기
                Matcher idMatcher = SERVICE_ID_PATTERN.matcher(lines[i]);
                if (idMatcher.find()) {
                    // 카운터 증가
                    counter.incrementAndGet();
                    
                    // @ServiceId 발견 - 이 라인부터 8줄 정도를 하나의 블록으로 처리
                    StringBuilder block = new StringBuilder(lines[i]);
                    for (int j = 1; j <= 8 && i + j < lines.length; j++) {
                        block.append("\n").append(lines[i + j]);
                    }
                    
                    String blockText = block.toString();
                    
                    // 블록 내에서 정보 추출
                    String serviceId = extractServiceId(blockText);
                    String serviceName = extractServiceName(blockText);
                    String[] dtos = extractDTOs(blockText);
                    
                    if (serviceId != null && dtos != null) {
                        String inDto = dtos[0];
                        String outDto = dtos[1];
                        
                        String filePath = escapeCsv(file.getParent() != null ? file.getParent().toString() : "");
                        String fileName = escapeCsv(file.getFileName().toString());
                        String svcId = escapeCsv(serviceId);
                        String svcName = escapeCsv(serviceName != null ? serviceName : "");
                        
                        synchronized (System.out) {
                            System.out.println(filePath + "," + fileName + "," + 
                                            svcId + "," + svcName + "," + 
                                            inDto + "," + outDto);
                        }
                    }
                }
            }
            
            // 해당 파일에서 찾은 ServiceId 개수 저장
            if (counter.get() > 0) {
                // 파일 경로와 파일명을 함께 저장
                String filePathName = file.toString();
                fileServiceCount.put(filePathName, counter);
            }
            
        } catch (IOException e) {
            System.err.println("파일 읽기 실패: " + file);
        }
    }
    
    private static String extractServiceId(String text) {
        Matcher matcher = SERVICE_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            String annotationContent = matcher.group(1);
            if (annotationContent.contains("value")) {
                Pattern valuePattern = Pattern.compile("value\\s*=\\s*\"([^\"]+)\"");
                Matcher valueMatcher = valuePattern.matcher(annotationContent);
                if (valueMatcher.find()) {
                    return valueMatcher.group(1);
                }
            } else {
                Pattern quotePattern = Pattern.compile("\"([^\"]+)\"");
                Matcher quoteMatcher = quotePattern.matcher(annotationContent);
                if (quoteMatcher.find()) {
                    return quoteMatcher.group(1);
                }
            }
        }
        return null;
    }
    
    private static String extractServiceName(String text) {
        Matcher matcher = SERVICE_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            String annotationContent = matcher.group(1);
            if (annotationContent.contains("value")) {
                Pattern valuePattern = Pattern.compile("value\\s*=\\s*\"([^\"]+)\"");
                Matcher valueMatcher = valuePattern.matcher(annotationContent);
                if (valueMatcher.find()) {
                    return valueMatcher.group(1);
                }
            } else {
                Pattern quotePattern = Pattern.compile("\"([^\"]+)\"");
                Matcher quoteMatcher = quotePattern.matcher(annotationContent);
                if (quoteMatcher.find()) {
                    return quoteMatcher.group(1);
                }
            }
        }
        return null;
    }
    
    private static String[] extractDTOs(String text) {
        Matcher matcher = METHOD_PATTERN.matcher(text);
        if (matcher.find()) {
            return new String[] { matcher.group(2), matcher.group(1) };
        }
        return null;
    }
    
    private static String escapeCsv(String value) {
        if (value == null) return "";
        boolean needQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        return needQuotes ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}