package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "lemma",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lemma", "site_id"}),
        indexes = @Index(name = "lemma_index", columnList = "lemma"))
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site siteId;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return Objects.equals(siteId, lemma1.siteId) && Objects.equals(lemma, lemma1.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, lemma);
    }
}
