package searchengine.model;

import lombok.*;

import java.util.Objects;
import javax.persistence.*;
import javax.persistence.Index;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "page",
        uniqueConstraints = @UniqueConstraint(columnNames = {"path", "site_id"}),
        indexes = @Index(name = "path_index", columnList = "path"))
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "site_id",  referencedColumnName = "id", nullable = false)
    private Site siteId;

    @Column(name = "path",
            nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content",
            columnDefinition = "MEDIUMTEXT",
            nullable = false)
    private String content;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return Objects.equals(siteId, page.siteId) && Objects.equals(path, page.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, path);
    }
}