package ao.lumen.pregao.feedback;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "feedback")
public class Feedback extends BaseEntity {
    @Column(name = "user_id")
    private UUID userId;
    private String category;
    private Integer rating;
    private String message;
    private String page;
    private String status = "NEW";

    protected Feedback() {}

    public Feedback(UUID userId, String category, Integer rating, String message, String page) {
        this.userId = userId;
        this.category = category;
        this.rating = rating;
        this.message = message;
        this.page = page;
    }

    public void setStatus(String status) { this.status = status; }
    public UUID getUserId() { return userId; }
    public String getCategory() { return category; }
    public Integer getRating() { return rating; }
    public String getMessage() { return message; }
    public String getPage() { return page; }
    public String getStatus() { return status; }
}
