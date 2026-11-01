package ao.lumen.pregao.authoring.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise extends BaseEntity {
    private String code;
    @Column(name = "author_id")
    private UUID authorId;
    private String topic;

    protected Exercise() {}

    public Exercise(String code, UUID authorId, String topic) {
        this.code = code;
        this.authorId = authorId;
        this.topic = topic;
    }

    public void setTopic(String topic) { this.topic = topic; }
    public String getCode() { return code; }
    public UUID getAuthorId() { return authorId; }
    public String getTopic() { return topic; }
}
