package index;

import com.google.common.base.Objects;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import worker.IThreadedWorker;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: cajias
 * Date: 01.07.14
 * Time: 21:34
 */
public class IndexWorker implements IThreadedWorker<Void> {
    private static Logger logger = LoggerFactory.getLogger(IndexWorker.class);
    private static final int THREAD_WAIT = 1000;

    private AtomicInteger counter;
    private final File outputDir;

    private boolean isDone = false;
    private IThreadedWorker<Document> extractorWorker;

    public IndexWorker(IThreadedWorker<Document> extractorWorker, File outputDir) {
        this.outputDir = outputDir;
        this.extractorWorker = extractorWorker;
    }

    @Override
    public void start() throws Exception {
        counter = new AtomicInteger();
        Directory dir = FSDirectory.open(outputDir);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48, analyzer);

        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        final IndexWriter writer = new IndexWriter(dir, iwc);
        ExecutorService executor = Executors.newCachedThreadPool();
        Future<Void> future = executor.submit(new Callable<Void>() {
            public Void call() throws IOException, InterruptedException {
                consumeQueue(writer);
                return null;
            }
        });

        future.get();
        writer.close();
        isDone = true;
    }

    private void consumeQueue(IndexWriter writer) throws InterruptedException, IOException {
        int emptyPollCount = 0;
        while (true) {
            Document document = extractorWorker.pollQueue();
            if (null == document) {
                if (extractorWorker.isDone()) {
                    break;
                }
                emptyPollCount++;
                logger.debug("Waiting on extractor. Retry count {}", emptyPollCount);
                Thread.sleep(THREAD_WAIT);
                continue;
            }
            emptyPollCount = 0;
            writer.addDocument(document);
            counter.addAndGet(1);
        }
    }

    @Override
    public Void pollQueue() {
        throw new UnsupportedOperationException("No queue for this worker");
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("counter", counter)
                .toString();
    }

}
