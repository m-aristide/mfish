package com.mfish.erp.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByTelephone(String telephone);

    List<Client> findByQuartierId(UUID quartierId);
}
