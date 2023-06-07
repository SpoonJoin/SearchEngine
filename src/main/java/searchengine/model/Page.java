package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "page")
@Getter
@Setter
@EqualsAndHashCode
@ToString
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
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    private Site site;
}