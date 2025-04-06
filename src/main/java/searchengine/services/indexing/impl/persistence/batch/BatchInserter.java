package searchengine.services.indexing.impl.persistence.batch;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class BatchInserter {

    public <T> void batchInsert(List<T> entities,
                                String sql,
                                BatchPreparedStatementSetter<T> setter,
                                DataSource dataSource,
                                int batchSize,
                                AtomicBoolean stopRequested) {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                int countingEntities = 0;
                for (int i = 0; i < entities.size() && !stopRequested.get(); i++) {
                    T entity = entities.get(i);
                    setter.setValues(ps, entity);
                    ps.addBatch();
                    countingEntities++;

                    if (countingEntities % batchSize == 0) {
                        ps.executeBatch();
                        assignGeneratedKeys(ps, entities, i - batchSize + 1, batchSize, setter);
                        log.info("{} lines out of {} have been uploaded.", countingEntities, entities.size());
                    }
                }

                if (countingEntities % batchSize != 0) {
                    int remaining = countingEntities % batchSize;
                    ps.executeBatch();
                    assignGeneratedKeys(ps, entities, entities.size() - remaining, remaining, setter);
                    log.info("Final {} rows inserted.", remaining);
                }

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }

        } catch (SQLException e) {
            log.error("SQL error during batch insert: {}", e.getMessage(), e);
        }
    }

    private <T> void assignGeneratedKeys(PreparedStatement ps, List<T> entities,
                                         int startIndex, int count,
                                         BatchPreparedStatementSetter<T> setter) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            int index = 0;
            while (rs.next() && index < count) {
                long id = rs.getLong(1);
                T entity = entities.get(startIndex + index);
                setter.setGeneratedId(entity, id);
                index++;
            }

            if (index != count) {
                log.warn("Expected {} generated keys but got {}", count, index);
            }
        }
    }
}
