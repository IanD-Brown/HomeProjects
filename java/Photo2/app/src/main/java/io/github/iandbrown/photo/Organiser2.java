package io.github.iandbrown.photo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import picocli.CommandLine;

@CommandLine.Command(name = "organiser", mixinStandardHelpOptions = true, version = "organiser 1.0",
        description = "Recursive search of the source directory for image files and upload to the destination in year/month day directories")
public class Organiser2 implements Callable<Integer> {
    private final SimpleDateFormat YEAR_DATE_FORMAT = new SimpleDateFormat("yyyy");
    private final SimpleDateFormat MONTH_DAY_DATE_FORMAT = new SimpleDateFormat("MM-dd ()");

    private File sourceDirectory;
    private File targetDirectory;
    private Date minDate;

    @CommandLine.Spec CommandLine.Model.CommandSpec spec; // injected by picocli

    @CommandLine.Parameters(index = "0", description = "Source directory.")
    public void setSourceDirectory(File file) {
        if (!file.isDirectory()) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Invalid source directory " + file.getAbsolutePath());
        }
        sourceDirectory = file;
    }
    @CommandLine.Parameters(index = "1", description = "Target directory.")
    public void setTargetDirectory(File file) {
        if (!file.isDirectory()) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Invalid target directory " + file.getAbsolutePath());
        }
        targetDirectory = file;
    }

    @CommandLine.Option(names = {"-m", "--minDate"}, description = "Minimum date filter (dd/MM/yyyy)")
    public void setMinDate(String value) {
        try {
            minDate = new SimpleDateFormat("dd/MM/yyy").parse(value);
        } catch (ParseException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Invalid date " + value);
        }
    }
    @CommandLine.Option(names = {"-t", "--test"}, description = "Test mode, default to false")
    private boolean test;

    @Override
    public Integer call() throws Exception {
        final AtomicBoolean hasDCIM = new AtomicBoolean();
        final AtomicBoolean hasImageContainer = new AtomicBoolean();

        Files.walkFileTree(sourceDirectory.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String pathString = dir.toString();
                hasDCIM.set(pathString.contains("DCIM"));
                hasImageContainer.set(pathString.endsWith("_PANA") || pathString.endsWith("Camera"));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (hasDCIM.get() &&
                        hasImageContainer.get() &&
                        (minDate == null || attrs.creationTime().toMillis() >= minDate.toInstant().toEpochMilli()) &&
                        !file.getFileName().toString().startsWith(".") &&
                        file.getFileName().toString().toUpperCase(Locale.ROOT).endsWith(".JPG")) {
                    handleSourceImage(file, attrs);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.SKIP_SUBTREE;
            }
        });

        return 0;
    }

    private void handleSourceImage(Path file, BasicFileAttributes attrs) {
        try {
            Date date = Date.from(attrs.lastModifiedTime().toInstant());
            String year = YEAR_DATE_FORMAT.format(date);
            String monthDay = MONTH_DAY_DATE_FORMAT.format(date);
            File yearFolder = new File(targetDirectory, year);
            File monthDayFolder = new File(yearFolder, monthDay);

            createIfAbsent(yearFolder);
            createIfAbsent(monthDayFolder);

            String sourceFileName = file.getFileName().toString();
            if (monthDayFolder.toPath().resolve(sourceFileName).toFile().exists()) {
                byte[] sha = getSha(file);
                int version = 1;
                while (monthDayFolder.toPath().resolve(sourceFileName).toFile().exists()) {
                    byte[] destinationSha = getSha(monthDayFolder.toPath().resolve(sourceFileName));

                    if (Arrays.equals(sha, destinationSha)) {
                        System.out.println("file existed with same content " + file.getFileName().toString());
                        return;
                    }
                    sourceFileName = versionFileName(file.getFileName().toString(), version++);
                }
            }

            if (test) {
                System.out.println("Test copy file " + file.getFileName().toString() + " to " + year + "/" + monthDay);
                return;
            }
             copyFile(file.toFile(), monthDayFolder.toPath().resolve(sourceFileName).toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static private void copyFile(File inputFile, File outputFile) throws IOException {
        if (!outputFile.exists()) {
            if (!outputFile.createNewFile())
                System.out.println("Problem creating file");
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile), 4096);
                 BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile), 4096)) {
                int c;

                while ((c = in.read()) != -1)
                    out.write(c);
            } catch (IOException e) {
                System.err.println("Error reading " + inputFile.getPath() + " to " + outputFile.getPath() + " " + e.getMessage());
                outputFile.delete();
            }
        }
    }
    private String versionFileName(String sourceName, int version) {
        int separator = sourceName.lastIndexOf('.');
        if (separator > 0) {
            return sourceName.substring(0, separator) + "_" + version + sourceName.substring(separator);
        }
        return sourceName + "_" + version;
    }

    private void createIfAbsent(File file) {
        if (!file.exists()) {
            System.out.println("Creating directory: " + file.getPath());
            if (!file.mkdir())
                System.out.println("Problem making directory");
        }
    }

    private byte[] getSha(Path source) throws IOException {
        byte[] data = Files.readAllBytes(source);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return digest.digest(data);
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new Organiser2()).execute(args));
    }
}
