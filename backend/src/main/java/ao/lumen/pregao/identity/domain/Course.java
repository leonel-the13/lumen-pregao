package ao.lumen.pregao.identity.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class Course extends BaseEntity {
    private String code;
    private String name;

    protected Course() {}

    public String getCode() { return code; }
    public String getName() { return name; }
}
