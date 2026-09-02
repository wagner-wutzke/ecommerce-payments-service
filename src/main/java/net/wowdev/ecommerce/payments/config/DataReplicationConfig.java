package net.wowdev.ecommerce.payments.config;

import net.wowdev.ecommerce.datareplication.config.ReplicationPersistenceConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ReplicationPersistenceConfig.class)
public class DataReplicationConfig {}
