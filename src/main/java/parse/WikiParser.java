package parse;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import worker.IThreadedWorker;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: cajias
 * Date: 01.07.14
 * Time: 08:51
 */
public class WikiParser implements IThreadedWorker<WikiPage> {
    private static Logger logger = LoggerFactory.getLogger(WikiParser.class);

    public static final int WORKING_QUEUE_CAPACITY = 100000;

    private Queue<WikiPage> workingQueue;
    private WikiXMLParser wikiXMLParser;
    private AtomicInteger counter;
    private boolean isDone = false;

    public WikiParser(String filename) {
        wikiXMLParser = WikiXMLParserFactory.getSAXParser(filename);
        workingQueue = new ArrayBlockingQueue<>(WORKING_QUEUE_CAPACITY);
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting document parser");
        wikiXMLParser.setPageCallback(new PageCallbackHandler() {
            @Override
            public void process(WikiPage page) {
                workingQueue.offer(page);
                counter.addAndGet(1);
            }
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<Void> futureTask = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                wikiXMLParser.parse();
                isDone = true;
                return null;
            }
        });
        counter = new AtomicInteger();
        executor.submit(futureTask);
    }

    @Override
    public WikiPage pollQueue() {
        return workingQueue.poll();
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("counter", counter)
                .toString();
    }
}
