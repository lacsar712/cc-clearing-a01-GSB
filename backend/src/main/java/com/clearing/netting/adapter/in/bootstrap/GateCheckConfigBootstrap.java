package com.clearing.netting.adapter.in.bootstrap;

import com.clearing.netting.domain.model.GateCheckConfig;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.port.out.GateCheckConfigRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首次启动时为四类门禁检查写入默认开关（全部开启）。
 */
@Component
public class GateCheckConfigBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GateCheckConfigBootstrap.class);

    private final GateCheckConfigRepositoryPort configRepository;

    public GateCheckConfigBootstrap(GateCheckConfigRepositoryPort configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (GateCheckType type : GateCheckType.values()) {
            if (configRepository.findByType(type).isEmpty()) {
                configRepository.save(GateCheckConfig.defaultOf(type));
            }
        }
        log.info("EOD gate check configs ensured: {} checks", GateCheckType.values().length);
    }
}
