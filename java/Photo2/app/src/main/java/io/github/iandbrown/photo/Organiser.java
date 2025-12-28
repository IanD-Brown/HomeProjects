import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


private static final SimpleDateFormat YEAR_DATE_FORMAT = new SimpleDateFormat("yyyy");
private static final SimpleDateFormat MONTH_DAY_DATE_FORMAT = new SimpleDateFormat("MM-dd ()");

private static int copied = 0;
private static final Map<String, List<String>> duplicates = new HashMap<>();

void main(String[] args) {
    if (args == null || args.length < 2) {
        IO.println("Usage: sourceDir destinationDir minDate(opt)");
        return;
    }

    try {
        copyFiles(args[0], args[1], args.length == 3 ? new SimpleDateFormat("dd/MM/yyy").parse(args[2]) : null);
        IO.println("Copied " + copied + " duplicates " + duplicates);
    } catch (IOException | ParseException e) {
        e.printStackTrace();
    }
}

static private void copyFiles(String sourceDir, String destinationDir, Date minDate) throws IOException {
    File sourceFile = new File(sourceDir);
    for (String imageFolder : Objects.requireNonNull(sourceFile.list((_, name) -> name.equals("Camera")))) {
        String innerPath = sourceDir + File.separator + imageFolder;
        File innerDirectory = new File(innerPath);
        for (String imageFile : Objects.requireNonNull(innerDirectory.list())) {
            copyToPath(new File(innerPath + File.separator + imageFile), destinationDir, minDate);
        }
    }
}

static private void copyToPath(File source, String destinationDir, Date minDate) {
    if (source.exists()) {
        try {
            Date date = new Date(source.lastModified());

            if (minDate != null && !date.after(minDate)) {
                return;
            }

            String year = YEAR_DATE_FORMAT.format(date);
            String monthDay = MONTH_DAY_DATE_FORMAT.format(date);
            File yearFolder = new File(destinationDir, year);
            File monthDayFolder = new File(yearFolder, monthDay);

            if (!yearFolder.exists()) {
                IO.println("Creating directory: " + yearFolder.getPath());
                if (!yearFolder.mkdir())
                    IO.println("Problem making directory");
            }

            if (!monthDayFolder.exists()) {
                IO.println("Creating directory: " + monthDayFolder.getPath());
                if (!monthDayFolder.mkdir())
                    IO.println("Problem making directory");
            }

            String[] contents = monthDayFolder.list();
            boolean exists = false;
            if (contents != null) {
                for (String content : contents) {
                    if (content.equals(source.getName())) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                IO.println("Copy file: " + source.getName() + " to: " + monthDayFolder.getPath());
                copyFile(source, new File(monthDayFolder, source.getName()));
                ++copied;
            } else {
                duplicates.computeIfAbsent(monthDayFolder.getName(), _ -> new ArrayList<>()).add(source.getName());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

static private void copyFile(File inputFile, File outputFile) throws IOException {
    if (!outputFile.exists()) {
        if (!outputFile.createNewFile())
            IO.println("Problem creating file");
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
