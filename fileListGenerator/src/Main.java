import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        List<String> musicFiles = new ArrayList<>();
        Map<String, AtomicInteger> extensionCounts = new HashMap<>();
        try (Stream<Path> stream = Files.walk(Paths.get(args[0]))) {
            stream.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(s -> !s.contains("Christmas"))
                    .filter(s -> !s.contains("Meditation"))
                    .filter(s -> !s.contains("DS_Store"))
                    .filter(s -> !s.endsWith(".m3u"))
                    .filter(s -> !s.endsWith(".jpg"))
                    .filter(s -> !s.contains("desktop.ini"))
                    .filter(s -> !s.contains("Thumbs.db"))
                    .map(s -> s.substring(args[0].length() + 1))
                    .forEach(musicFiles::add);
            Collections.shuffle(musicFiles);
            for (int i = 0; i < 7; ++i) {
                try (FileWriter fileWriter = new FileWriter(args[0] + File.separator+i+".m3u")) {
                    fileWriter.write("#EXTM3U\n");
                    for (int j = i; j < musicFiles.size(); j += 7) {
                        fileWriter.write(musicFiles.get(j));
                        fileWriter.write("\n");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(musicFiles.size());
        musicFiles.forEach(s -> {
            if (s.endsWith("zip")) {
                System.out.println(s);
            }
            int lastFullStop = s.lastIndexOf('.');
            String extension = lastFullStop > 0 ? s.substring(lastFullStop + 1) : "";
            extensionCounts.computeIfAbsent(extension, k -> new AtomicInteger()).incrementAndGet();
        });
        extensionCounts.forEach((e,c) -> System.out.println(e + " " + c));
        musicFiles.stream().filter(s -> !s.contains(".")).forEach(System.out::println);
    }
}