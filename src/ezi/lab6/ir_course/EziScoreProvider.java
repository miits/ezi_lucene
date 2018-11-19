package ezi.lab6.ir_course;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.search.Query;

import java.io.IOException;

public class EziScoreProvider extends CustomScoreProvider {
    public EziScoreProvider(LeafReaderContext context) {
        super(context);
    }

    @Override
    public float customScore(int doc, float subQueryScore, float valSrcScore) throws IOException {
        return super.customScore(doc, subQueryScore, valSrcScore);
    }
}
