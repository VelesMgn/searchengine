package searchengine.services.indexing.impl.persistence.batch.setters;

import searchengine.model.Page;
import searchengine.services.indexing.impl.persistence.batch.BatchPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class PageStatementSetter implements BatchPreparedStatementSetter<Page> {

    @Override
    public void setValues(PreparedStatement ps, Page page) throws SQLException {
        ps.setString(1, page.getPath());
        ps.setLong(2, page.getSiteId().getId());
        ps.setLong(3, page.getCode());
        ps.setString(4, page.getContent());
    }

    @Override
    public void setGeneratedId(Page page, long id) {
        page.setId((int) id);
    }
}
