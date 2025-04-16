import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ServiceScanner {
    // 정규식 패턴 정의
    private static final Pattern SERVICE_PATTERN = Pattern.compile(
        "@ServiceId\\([^)]*value\\s*=\\s*\"(.*?)\"[^)]*\\)\\s*" +     // value="..." 추출
        "@ServiceName\\(\"(.*?)\"\\)\\s*" +                          // name 추출
        "(?:@Override\\s*)?" +                                       // @Override는 있어도 되고 없어도 됨
        "public\\s+(\\w+)\\s+\\w+\\s*\\(\\s*(\\w+)\\s+\\w+\\)",       // 리턴타입, 파라미터타입
        Pattern.DOTALL
    );

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("📁 폴더 경로를 인자로 입력해주세요.");
            System.err.println("예: java ServiceScanner ./src");
            return;
        }

        String folderPath = args[0];
        System.out.println("🔍 처리 중인 폴더: " + folderPath);

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(ServiceScanner::processJavaFile);
        } catch (IOException e) {
            System.err.println("❌ 유효한 폴더 경로가 아닙니다: " + folderPath);
        }
    }

    private static void processJavaFile(Path filePath) {
        try {
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Matcher matcher = SERVICE_PATTERN.matcher(content);

            while (matcher.find()) {
                System.out.println("✅ 파일: " + filePath.getFileName());
                System.out.println("  🔹 serviceId: " + matcher.group(1));
                System.out.println("  🔹 serviceName: " + matcher.group(2));
                System.out.println("  🔹 outDto: " + matcher.group(3));
                System.out.println("  🔹 inDto: " + matcher.group(4));
            }
        } catch (IOException e) {
            System.err.println("❌ 파일 읽기 실패: " + filePath);
        }
    }
}
