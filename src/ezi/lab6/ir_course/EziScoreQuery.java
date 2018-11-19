package ezi.lab6.ir_course;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;

public class EziScoreQuery extends CustomScoreQuery {
    public EziScoreQuery(Query subQuery) {
        super(subQuery);
    }

    public EziScoreQuery(Query subQuery, FunctionQuery scoringQuery) {
        super(subQuery, scoringQuery);
    }

    public EziScoreQuery(Query subQuery, FunctionQuery... scoringQueries) {
        super(subQuery, scoringQueries);
    }

    @Override
    protected CustomScoreProvider getCustomScoreProvider(LeafReaderContext context) throws IOException {
        return new EziScoreProvider(context);
    }
}
