package Pages;

import Database.IdbManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

public class FileUrlListHandler implements IUrlListHandler {
    private final PrintWriter UrlListFile;
    private final HashSet<String> UrlSet = new HashSet<>();

    public FileUrlListHandler(String Filename) {

        final File UrlList = new File("." + File.separator + "Files" + File.separator + Filename);

        PrintWriter _UrlListFile;
        try {
            _UrlListFile = new PrintWriter((new FileWriter(UrlList, true)), true);
        } catch (IOException ex) {
            ex.printStackTrace();
            _UrlListFile = null;
        }

        UrlListFile = _UrlListFile;

        try {
            Files.lines(Path.of(UrlList.getPath())).forEach(UrlSet::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(String Url) {
        UrlSet.add(Url);
        UrlListFile.println(Url);
    }

    @Override
    public boolean contains(String Url) {
        return UrlSet.contains(Url);
    }
}
