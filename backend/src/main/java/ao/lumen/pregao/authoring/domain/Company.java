package ao.lumen.pregao.authoring.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Empresa fictícia aprovada para exercícios (fictional é sempre true; a base de dados garante-o). */
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {
    private String name;
    private String sector;
    private String description;
    private boolean fictional = true;

    protected Company() {}

    public String getName() { return name; }
    public String getSector() { return sector; }
    public String getDescription() { return description; }
    public boolean isFictional() { return fictional; }
}
