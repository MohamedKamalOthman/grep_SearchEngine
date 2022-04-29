package Pages;

import Crawler.DocumentWrapper;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import Database.IdbManager;

import java.io.*;

public class FileHtmlPageSaver implements IHtmlPageSaver {
    private final String PathName;
    private IdbManager Manager;

    public FileHtmlPageSaver(String pathName,IdbManager manager) {
        PathName = pathName;
        Manager = manager;
    }

    @Override
    public void save(DocumentWrapper WDoc, @NotNull String Url) {
        //Add to database URL mapping to files
        File HtmlDoc = new File(PathName + WDoc.crc + ".html");

        try {
            if (!HtmlDoc.createNewFile())
                System.out.println("Couldn't Create File");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(HtmlDoc);
            myWriter.write(WDoc.html);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
            Manager.savePage(Url,WDoc.crc,false);
            System.out.println("Successfully wrote to Collection.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }
}
