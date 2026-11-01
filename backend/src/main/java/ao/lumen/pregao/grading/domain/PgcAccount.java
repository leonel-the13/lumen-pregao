package ao.lumen.pregao.grading.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "pgc_accounts")
public class PgcAccount extends BaseEntity {
    private String version;
    private String code;
    private String name;
    @Column(name = "class_code")
    private String classCode;
    @Column(name = "class_name")
    private String className;
    private boolean placeholder;

    protected PgcAccount() {}

    public String getVersion() { return version; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getClassCode() { return classCode; }
    public String getClassName() { return className; }
    public boolean isPlaceholder() { return placeholder; }
}
