package Pages;

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
    public void save(Document doc, @NotNull String Url) {
        //Add to database URL mapping to files
        int hash = Url.hashCode();
        File HtmlDoc = new File(PathName + hash + ".html");

        try {
            if (!HtmlDoc.createNewFile())
                System.out.println("Couldn't Create File");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(HtmlDoc);
            myWriter.write(doc.html());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
            Manager.savePage(Url,hash,false);
            System.out.println("Successfully wrote to Collection.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }
}
