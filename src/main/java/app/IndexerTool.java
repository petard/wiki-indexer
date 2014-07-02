package app;

import edu.jhu.nlp.wikipedia.WikiPage;
import extract.WikiExtractor;
import index.IndexWorker;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parse.WikiParser;
import worker.IThreadedWorker;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * User: cajias
 * Date: 01.07.14
 * Time: 21:34
 */
public class IndexerTool {

    private static Logger logger = LoggerFactory.getLogger(IndexerTool.class);

    public static void main(String[] args) throws Exception {

        File wikipedia = null;
        File outputDir = null;
        int debugMessageTimer = 10000;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--input") || arg.equals("-i")) {
                wikipedia = new File(args[i + 1]);
                i++;
            } else if (arg.equals("--output") || arg.equals("-o")) {
                outputDir = new File(args[i + 1]);
                i++;
            } else if (arg.equals("--debugTimer") || arg.equals("-dt")) {
                debugMessageTimer = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        if (null == wikipedia || null == outputDir) {
            printUsage();
            System.exit(-1);
        }

        IThreadedWorker<WikiPage> parseWorker = new WikiParser(wikipedia.getAbsolutePath());
        parseWorker.start();
        IThreadedWorker<Document> wikiExtractor = new WikiExtractor(parseWorker);
        wikiExtractor.start();
        final IThreadedWorker<Void> indexWorker = new IndexWorker(wikiExtractor, outputDir);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<Void> futureTask = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                indexWorker.start();
                return null;
            }
        });
        executor.submit(futureTask);

        long startTime = System.currentTimeMillis();

        while (!parseWorker.isDone() &&
                !wikiExtractor.isDone() &&
                !indexWorker.isDone()) {
            long currTime = System.currentTimeMillis() - startTime;
            logger.info("--------------------");
            logger.info("Articles parsed: {}", parseWorker);
            logger.info("Docs extracted: {}", wikiExtractor);
            logger.info("Docs indexed: {}", indexWorker);
            logger.info("Elapsed: {} ms", currTime);
            if (indexWorker.isDone()) {
                break;
            }
            Thread.sleep(debugMessageTimer);
        }
        long currTime = System.currentTimeMillis() - startTime;
        logger.info("Finished Indexing. Elapsed time: {} ms", currTime);
    }

    private static void printUsage() {
        System.err.println("Usage: java -cp <...> app.IndexerTool --input|-i <Path to Wikipedia XML file> " +
                "[--output|-o <Output Path>]");
    }

}
