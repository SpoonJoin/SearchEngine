package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "page")
@Getter
@Setter
@NoArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private int id;
    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE KEY p_index (path(512), site_id)")
    private String path;
    @Column(columnDefinition = "INT NOT NULL", name = "site_id", insertable = false, updatable = false)
    private int siteId;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;
    @ManyToOne(fetch = FetchType.LAZY)
    private Site site;
}
