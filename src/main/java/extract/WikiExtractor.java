package extract;

import com.google.common.base.Objects;
import edu.jhu.nlp.wikipedia.WikiPage;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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
 * Date: 30.06.14
 * Time: 08:26
 */
public class WikiExtractor implements IThreadedWorker<Document>{
    private static Logger logger = LoggerFactory.getLogger(WikiExtractor.class);

    private static final int MAX_RETRIES = 5;
    private static final int WORKING_QUEUE_CAPACITY = 1000;
    private static final int THREAD_WAIT = 10000;
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTRIBUTOR = "contributor";
    private static final String FIELD_BODY = "body";

    private AtomicInteger counter;

    private Queue<Document> workingQueue;

    private final IThreadedWorker<WikiPage> parseWorker;

    public WikiExtractor(IThreadedWorker<WikiPage> parseWorker) {
        this.parseWorker = parseWorker;
        workingQueue = new ArrayBlockingQueue<Document>(WORKING_QUEUE_CAPACITY);
    }

    public void start() throws Exception {
        logger.info("Started extractor thread");
        ExecutorService executor = Executors.newCachedThreadPool();
        FutureTask<Void> futureTask = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                consumeQueue();
                return null;
            }
        });
        counter = new AtomicInteger();
        executor.submit(futureTask);
    }

    @Override
    public int processCount() {
        return null == counter ? 0 : counter.get();
    }

    private void consumeQueue() throws InterruptedException {
        int emptyPollCount = 0;
        while (true) {
            WikiPage wikiPage = parseWorker.pollQueue();
            if (null == wikiPage) {
                emptyPollCount++;
                logger.info("Waiting on parser. Retry count {}", emptyPollCount);
                Thread.sleep(THREAD_WAIT);
                continue;
            }
            if (MAX_RETRIES <= emptyPollCount) {
                logger.info("Retries exhausted.");
                break;
            }
            emptyPollCount = 0;
            if (null == wikiPage.getContributor()) {
                continue;
            }
            Document doc = getDocument(wikiPage);
            workingQueue.offer(doc);
            counter.addAndGet(1);
        }
    }

    @Override
    public Document pollQueue() {
        return workingQueue.poll();
    }

    private Document getDocument(WikiPage wikiPage) {
        Document document = new Document();
        Field title = new StringField(FIELD_TITLE, wikiPage.getTitle(), Field.Store.YES);
        document.add(title);
        Field contributor = new StringField(FIELD_CONTRIBUTOR, wikiPage.getContributor(), Field.Store.NO);
        document.add(contributor);
        Field body = new TextField(FIELD_BODY, wikiPage.getText(), Field.Store.NO);
        document.add(body);
        return document;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("counter", counter)
                .toString();
    }
}
