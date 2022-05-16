package pages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Stream;

public class FileUrlListHandler implements IUrlListHandler {
    private final PrintWriter urlListFile;
    private final HashSet<String> urlSet = new HashSet<>();

    public FileUrlListHandler(String fileName) {

        final File urlList = new File("." + File.separator + "Files" + File.separator + fileName);

        PrintWriter urlListFileTemp;
        try {
            urlListFileTemp = new PrintWriter((new FileWriter(urlList, true)), true);
        } catch (IOException ex) {
            ex.printStackTrace();
            urlListFileTemp = null;
        }

        urlListFile = urlListFileTemp;

        try (Stream<String> lines = Files.lines(Path.of(urlList.getPath()))) {
            lines.forEach(urlSet::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(String url) {
        urlSet.add(url);
        urlListFile.println(url);
    }

    @Override
    public boolean contains(String url) {
        return urlSet.contains(url);
    }
}
