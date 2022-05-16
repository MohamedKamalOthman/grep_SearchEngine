package pages;

import crawler.DocumentWrapper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConcurrentHTMLPageSaver implements Runnable, IHtmlPageSaver {
    private boolean interrupted = false;
    private final IHtmlPageSaver htmlPageSaver;
    private final Thread thread;
    private final BlockingQueue<SaveTask> pageSavingTaskQueue = new LinkedBlockingQueue<>();
    public record SaveTask(DocumentWrapper wDoc, String url) {}

    public ConcurrentHTMLPageSaver(IHtmlPageSaver htmlPageSaver) {
        this.htmlPageSaver = htmlPageSaver;
        thread = new Thread(this);
    }

    public void start() {
        thread.start();
    }

    public Thread getThread() {
        return thread;
    }

    public void interrupt() {
        interrupted = true;
    }

    @Override
    public void save(DocumentWrapper wDoc, String url) {
        //if(!thread.isAlive())
            //throw new IllegalStateException("Saving document to a closed thread");

        try {
            pageSavingTaskQueue.put(new SaveTask(wDoc, url));
        }
        catch (InterruptedException ie) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        if(htmlPageSaver == null)
            throw new IllegalStateException("ConcurrentHTMLPageSaver ran without a reference to an HTMLPageSaver");
        SaveTask currentTask;
        while(!interrupted || !pageSavingTaskQueue.isEmpty()) {
            try {
                currentTask = pageSavingTaskQueue.poll(1, TimeUnit.SECONDS);
                if(currentTask == null)
                    continue;
            }
            catch (InterruptedException ie) {
                thread.interrupt();
                return;
            }

            htmlPageSaver.save(currentTask.wDoc, currentTask.url);
        }
    }
}
