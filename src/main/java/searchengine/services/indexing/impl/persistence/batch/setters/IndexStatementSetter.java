package searchengine.services.indexing.impl.persistence.batch.setters;

import searchengine.model.Index;
import searchengine.services.indexing.impl.persistence.batch.BatchPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class IndexStatementSetter implements BatchPreparedStatementSetter<Index> {

    @Override
    public void setValues(PreparedStatement ps, Index index) throws SQLException {
        ps.setLong(1, index.getPageId().getId());
        ps.setLong(2, index.getLemmaId().getId());
        ps.setFloat(3, index.getRank());
    }

    @Override
    public void setGeneratedId(Index index, long id) {
        index.setId((int) id);
    }
}
