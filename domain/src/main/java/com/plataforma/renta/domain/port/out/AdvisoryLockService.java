package com.plataforma.renta.domain.port.out;

import java.util.List;
import java.util.UUID;

public interface AdvisoryLockService {

    void acquireLocks(List<UUID> herramientaIds);

    void releaseLocks();
}