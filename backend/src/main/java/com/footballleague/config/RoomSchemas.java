package com.footballleague.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Oda şemalarının tablolarını oluşturur ve günceller. Hibernate açılışta yalnızca varsayılan şemayı
 * (ddl-auto) günceller; odalar ayrı şemalarda olduğu için aynı işlem her oda şeması için burada yapılır.
 * Bunun için Hibernate'in eşleme bilgisi (Metadata) gerekir; açılışta Integrator olarak yakalanır.
 */
@Component
public class RoomSchemas implements Integrator, HibernatePropertiesCustomizer {

    private volatile Metadata metadata;
    private volatile SessionFactoryImplementor sessionFactory;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER,
                (IntegratorProvider) () -> List.of(this));
    }

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {
        this.metadata = metadata;
        this.sessionFactory = sessionFactory;
    }

    /** Şemayı ve tüm tabloları oluşturur. */
    public void create(String schema) {
        run(schema, Action.CREATE_ONLY, true);
    }

    /** Eksik tabloları / sütunları ekler (ddl-auto: update gibi; sütun silmez). */
    public void update(String schema) {
        run(schema, Action.UPDATE, false);
    }

    private void run(String schema, Action action, boolean createSchema) {
        Map<String, Object> settings = new HashMap<>(sessionFactory.getProperties());
        settings.remove(AvailableSettings.HBM2DDL_AUTO);
        settings.put(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, action);
        settings.put(AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, createSchema);
        settings.put(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE);
        settings.put(AvailableSettings.DEFAULT_SCHEMA, schema);
        SchemaManagementToolCoordinator.process(metadata, sessionFactory.getServiceRegistry(), settings, drop -> {
        });
    }
}
