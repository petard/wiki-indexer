package app;

import extract.WikiExtractor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Simple command-line based search demo.
 */
public class SearchTool {


    /**
     * Simple command-line based search demo.
     */
    public static void main(String[] args) throws Exception {
        String usage =
                "Usage:\tjava app.SearchTool [-index dir].";
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

        String index = "index";
        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                index = args[i + 1];
                i++;
            }
        }

        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
//        QueryParser queryParser = new QueryParser(Version.LUCENE_48, WikiExtractor.FIELD_CONTRIBUTOR, analyzer);
        String[] searchFields = {WikiExtractor.FIELD_BODY, WikiExtractor.FIELD_CONTRIBUTOR};
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_48, searchFields, analyzer);
        queryParser.setDefaultOperator(QueryParser.Operator.OR);
        while (true) {
            System.out.print("Enter query:\t");

            String line = in.readLine();

            if (line == null || line.length() == -1) {
                break;
            }

            line = line.trim();
            if (line.length() == 0) {
                break;
            }

            Query query = queryParser.parse(line);
            System.out.println("Searching for: " + query.toString());
            ScoreDoc[] hits = searcher.search(query, null, 10).scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                System.out.println(doc.get(WikiExtractor.FIELD_TITLE));
            }

        }
        reader.close();
    }
}
























































