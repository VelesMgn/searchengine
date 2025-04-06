package searchengine.model;

import javax.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "`index`",
        uniqueConstraints = @UniqueConstraint(columnNames = {"page_id", "lemma_id"}),
        indexes = {
                @javax.persistence.Index(name = "idx_lemma", columnList = "lemma_id"),
                @javax.persistence.Index(name = "idx_page_lemma", columnList = "page_id, lemma_id")
        }
)
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page pageId;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemmaId;

    @Column(name = "`rank`", nullable = false)
    private float rank;
}
