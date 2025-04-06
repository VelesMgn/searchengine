package searchengine.services.indexing.impl.persistence.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface BatchPreparedStatementSetter<T> {
    void setValues(PreparedStatement ps, T entity) throws SQLException;

    void setGeneratedId(T entity, long id);
}

