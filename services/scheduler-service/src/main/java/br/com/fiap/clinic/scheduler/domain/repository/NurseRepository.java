package br.com.fiap.clinic.scheduler.domain.repository;

import br.com.fiap.clinic.scheduler.domain.entity.Nurse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NurseRepository extends JpaRepository<Nurse, UUID> {
}

