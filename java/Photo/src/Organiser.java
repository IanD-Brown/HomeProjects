import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class Organiser {

    private static final SimpleDateFormat YEAR_DATE_FORMAT = new SimpleDateFormat("yyyy");
    private static final SimpleDateFormat MONTH_DAY_DATE_FORMAT = new SimpleDateFormat("MM-dd ()");

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Usage: sourceDir, destDir");
        }

        try {
            copyFiles(args[0], args[1]);
        } catch (IOException e) {
            e.  printStackTrace();
        }
    }

    static private void copyFiles(String sourceDir, String destDir) throws IOException {
        File sourceFile = new File(sourceDir);
        for (String imageFolder : Objects.requireNonNull(sourceFile.list((_, name) -> name.endsWith("_PANA")))) {
            String innerPath = sourceDir + File.separator + imageFolder;
            File innerDirectory = new File(innerPath);
            for (String imageFile : Objects.requireNonNull(innerDirectory.list())) {
                copyToPath(new File(innerPath + File.separator + imageFile), destDir);
            }
        }
    }

    //    static private boolean removeCopyWithBadDate(File source, String destDir) throws IOException {
//        if (source.exists()) {
//            BasicFileAttributes attr = Files.readAttributes(source.toPath(), BasicFileAttributes.class);
//            Date created = new Date(attr.creationTime().toMillis());
//            Date lastModified = new Date(source.lastModified());
//
//            String createdYear = YEAR_DATE_FORMAT.format(created);
//            String modifiedYear = YEAR_DATE_FORMAT.format(lastModified);
//            String createdMonthDay = MONTH_DAY_DATE_FORMAT.format(created);
//            String modifiedMonthDay = MONTH_DAY_DATE_FORMAT.format(lastModified);
//
//            if (createdYear.equals(modifiedYear) && createdMonthDay.equals(modifiedMonthDay)) {
//                return false;
//            }
//            System.out.println("In wrong folder "+source.getName()+" "
//                                       +modifiedYear+"/"+modifiedMonthDay+" vs "
//                                       +createdYear+"/"+createdMonthDay);
//            // TODO, remove file...
//            return true;
//        }
//        return false;
//    }
//
    static private void copyToPath(File source, String destDir) {
        if (source.exists()) {
            try {
                Date date = new Date(source.lastModified());
                String year = YEAR_DATE_FORMAT.format(date);
                String monthDay = MONTH_DAY_DATE_FORMAT.format(date);

                File yearFolder = new File(destDir, year);
                File monthDayFolder = new File(yearFolder, monthDay);

                if (!yearFolder.exists()) {
                    System.out.println("Creating directory: " + yearFolder.getPath());
                    if (!yearFolder.mkdir())
                        System.out.println("Problem making directory");
                }

                if (!monthDayFolder.exists()) {
                    File aFile = new File(monthDayFolder, "xx");
                    System.out.println("Creating directory: " + monthDayFolder.getPath());
                    if (!monthDayFolder.mkdir())
                        System.out.println("Problem making directory");
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
                    System.out.println("Copy file: " + source.getName() + " to: " + monthDayFolder.getPath());
                    copyFile(source, new File(monthDayFolder, source.getName()));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static private void copyFile(File inputFile, File outputFile) throws IOException {
        if (!outputFile.exists()) {
            if (!outputFile.createNewFile())
                System.out.println("Problem creating file");
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile), 4096);
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile), 4096)) {
                // FileReader in = new FileReader(inputFile);
                //FileWriter out = new FileWriter(outputFile);
                int c;

                while ((c = in.read()) != -1)
                    out.write(c);
            } catch (IOException e) {
                System.err.println("Error reading " + inputFile.getPath() + " to " + outputFile.getPath() + " " + e.getMessage());
                outputFile.delete();
            }
        }
    }
}
