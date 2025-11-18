package br.com.fiap.clinic.notification.domain.repository;

import br.com.fiap.clinic.notification.domain.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
}