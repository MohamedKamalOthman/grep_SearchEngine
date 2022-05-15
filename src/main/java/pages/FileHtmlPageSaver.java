package pages;

import crawler.DocumentWrapper;
import org.jetbrains.annotations.NotNull;
import database.DBManager;

import java.io.*;

public class FileHtmlPageSaver implements IHtmlPageSaver {
    private final String pathName;
    private final DBManager manager;

    public FileHtmlPageSaver(String pathName, DBManager manager) {
        this.pathName = pathName;
        this.manager = manager;
    }

    @Override
    public void save(DocumentWrapper wDoc, @NotNull String url) {
        //Add to database URL mapping to files
        File htmlFile = new File(pathName + wDoc.crc + ".html");

        try {
            if (!htmlFile.createNewFile())
                System.out.println("Couldn't Create File");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileWriter myWriter = new FileWriter(htmlFile)) {
            myWriter.write(wDoc.html);
            System.out.println("Successfully wrote to the file.");
            manager.savePage(url, wDoc.crc, false);
            System.out.println("Successfully wrote to Collection.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
