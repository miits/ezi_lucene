package ezi.lab6.ir_course;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LuceneSearchSimApp {

    private static String indexPath = "rss_index";

    public LuceneSearchSimApp() {

    }

    public void index(List<RssFeedDocument> docs) throws IOException {
        // implement the Lucene indexing here
        Path iPath = Paths.get(indexPath);
        Directory directory = FSDirectory.open(iPath);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter;
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        for(RssFeedDocument doc: docs) {
            Document indexableFields = new Document();
            indexableFields.add(new TextField("title", doc.getTitle(), Field.Store.YES));
            indexableFields.add(new TextField("description", new StringReader(doc.getDescription())));
            indexableFields.add(new StringField("date", simpleDateFormat.format(doc.getPubDate()), Field.Store.YES));
            indexWriter.addDocument(indexableFields);
        }
    }

    public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) throws ParseException, IOException {

        printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);

        List<String> results = new LinkedList<String>();

        // implement the Lucene search here
        Query query = new EziScoreQuery(queryFromLists(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate));
        Path path = Paths.get(indexPath);
        Directory directory = FSDirectory.open(path);
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        TopDocs topDocs = indexSearcher.search(query, 30);
        ScoreDoc hits[] = topDocs.scoreDocs;
        for(ScoreDoc hit: hits) {
            results.add(indexReader.document(hit.doc).getField("title").stringValue());
        }
        return results;
    }

    public Query queryFromLists(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) throws ParseException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser titleQueryParser = new QueryParser("title", analyzer);
        QueryParser descQueryParser = new QueryParser("description", analyzer);
        QueryParser dateQueryParser = new QueryParser("date", analyzer);
        if (inTitle != null) builder.add(titleQueryParser.parse(String.join(" AND ", inTitle)), BooleanClause.Occur.MUST);
        if (inDescription != null) builder.add(descQueryParser.parse(String.join(" AND ", inDescription)), BooleanClause.Occur.MUST);
        if (notInTitle != null) builder.add(titleQueryParser.parse(String.join(" OR ", notInTitle)), BooleanClause.Occur.MUST_NOT);
        if (notInDescription != null) builder.add(descQueryParser.parse(String.join(" OR ", notInDescription)), BooleanClause.Occur.MUST_NOT);
        String dateQueryText = buildDateQueryText(startDate, endDate);
        if (dateQueryText != "") builder.add(dateQueryParser.parse(dateQueryText), BooleanClause.Occur.MUST);
        return builder.build();
    }

    public String buildDateQueryText(String fromDate, String toDate) {
        String queryText = "";
        if (fromDate != null || toDate != null) {
            String from = fromDate != null ? fromDate : "*";
            String to = toDate != null ? toDate : "*";
            queryText = String.format("date:[%s TO %s]", from, to);
        }
        return queryText;
    }

    public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) {
        System.out.print("Search (");
        if (inTitle != null) {
            System.out.print("in title: "+inTitle);
            if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (notInTitle != null) {
            System.out.print("not in title: "+notInTitle);
            if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (inDescription != null) {
            System.out.print("in description: "+inDescription);
            if (notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (notInDescription != null) {
            System.out.print("not in description: "+notInDescription);
            if (startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (startDate != null) {
            System.out.print("startDate: "+startDate);
            if (endDate != null)
                System.out.print("; ");
        }
        if (endDate != null)
            System.out.print("endDate: "+endDate);
        System.out.println("):");
    }

    public void printResults(List<String> results) {
        if (results.size() > 0) {
            Collections.sort(results);
            for (int i=0; i<results.size(); i++)
                System.out.println(" " + (i+1) + ". " + results.get(i));
        }
        else
            System.out.println(" no results");
    }

    public static void main(String[] args) throws IOException, ParseException {
        if (args.length > 0) {
            LuceneSearchSimApp engine = new LuceneSearchSimApp();

            RssFeedParser parser = new RssFeedParser();
            parser.parse(args[0]);
            List<RssFeedDocument> docs = parser.getDocuments();

            engine.index(docs);

            List<String> inTitle;
            List<String> notInTitle;
            List<String> inDescription;
            List<String> notInDescription;
            List<String> results;

            // 1) search documents with words "kim" and "korea" in the title
            inTitle = new LinkedList<String>();
            inTitle.add("kim");
            inTitle.add("korea");
            results = engine.search(inTitle, null, null, null, null, null);
            engine.printResults(results);

            // 2) search documents with word "kim" in the title and no word "korea" in the description
            inTitle = new LinkedList<String>();
            notInDescription = new LinkedList<String>();
            inTitle.add("kim");
            notInDescription.add("korea");
            results = engine.search(inTitle, null, null, notInDescription, null, null);
            engine.printResults(results);

            // 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and "" in the description
            inTitle = new LinkedList<String>();
            inTitle.add("us");
            notInTitle = new LinkedList<String>();
            notInTitle.add("dawn");
            inDescription = new LinkedList<String>();
            inDescription.add("american");
            inDescription.add("confession");
            results = engine.search(inTitle, notInTitle, inDescription, null, null, null);
            engine.printResults(results);

            // 4) search documents whose publication date is 2011-12-18
            results = engine.search(null, null, null, null, "2011-12-18", "2011-12-18");
            engine.printResults(results);

            // 5) search documents with word "video" in the title whose publication date is 2000-01-01 or later
            inTitle = new LinkedList<String>();
            inTitle.add("video");
            results = engine.search(inTitle, null, null, null, "2000-01-01", null);
            engine.printResults(results);

            // 6) search documents with no word "canada" or "iraq" or "israel" in the description whose publication date is 2011-12-18 or earlier
            notInDescription = new LinkedList<String>();
            notInDescription.add("canada");
            notInDescription.add("iraq");
            notInDescription.add("israel");
            results = engine.search(null, null, null, notInDescription, null, "2011-12-18");
            engine.printResults(results);
        }
        else
            System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
    }
}
