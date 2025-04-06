package searchengine.services.indexing.impl.persistence.batch.setters;

import searchengine.model.Lemma;
import searchengine.services.indexing.impl.persistence.batch.BatchPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class LemmaStatementSetter implements BatchPreparedStatementSetter<Lemma> {

    @Override
    public void setValues(PreparedStatement ps, Lemma lemma) throws SQLException {
        ps.setString(1, lemma.getLemma());
        ps.setLong(2, lemma.getSiteId().getId());
        ps.setLong(3, lemma.getFrequency());
    }

    @Override
    public void setGeneratedId(Lemma lemma, long id) {
        lemma.setId((int) id);
    }
}