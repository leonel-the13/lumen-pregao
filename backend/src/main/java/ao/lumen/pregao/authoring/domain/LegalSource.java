package ao.lumen.pregao.authoring.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Diploma, norma ou obra de referência. `confirmed=false` aparece como «a confirmar». */
@Entity
@Table(name = "legal_sources")
public class LegalSource extends BaseEntity {
    private String title;
    private String identifier;
    private String version;
    @Column(name = "published_at")
    private LocalDate publishedAt;
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    private String url;
    private boolean confirmed;

    protected LegalSource() {}

    public LegalSource(String title, String identifier, String version) {
        this.title = title;
        this.identifier = identifier;
        this.version = version;
    }

    public void update(String title, String identifier, String version, LocalDate publishedAt, LocalDate effectiveFrom,
                       LocalDate effectiveTo, String url, boolean confirmed) {
        this.title = title;
        this.identifier = identifier;
        this.version = version;
        this.publishedAt = publishedAt;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.url = url;
        this.confirmed = confirmed;
    }

    public String getTitle() { return title; }
    public String getIdentifier() { return identifier; }
    public String getVersion() { return version; }
    public LocalDate getPublishedAt() { return publishedAt; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public String getUrl() { return url; }
    public boolean isConfirmed() { return confirmed; }
}
