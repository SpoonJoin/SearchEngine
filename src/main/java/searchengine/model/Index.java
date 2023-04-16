package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "index_tab")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private Lemma lemma;
    @Column(columnDefinition = "FLOAT NOT NULL", name = "index_rank")
    private float rank;

//    @Column(columnDefinition = "INT NOT NULL", name = "page_id",
//            insertable = false, updatable = false)
//    private int pageId;
//    @Column(columnDefinition = "INT NOT NULL", name = "lemma_id",
//            insertable = false, updatable = false)
//    private int lemmaId;
}
