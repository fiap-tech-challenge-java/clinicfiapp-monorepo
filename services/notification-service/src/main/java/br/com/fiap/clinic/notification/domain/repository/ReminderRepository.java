package br.com.fiap.clinic.notification.domain.repository;

import br.com.fiap.clinic.notification.domain.entity.ReminderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<ReminderEntity, UUID> {
}
