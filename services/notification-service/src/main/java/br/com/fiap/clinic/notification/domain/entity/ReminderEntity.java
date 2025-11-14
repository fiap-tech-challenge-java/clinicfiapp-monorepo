package br.com.fiap.clinic.notification.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "reminder")
public class ReminderEntity {

    //A discutir
    @Id
    private UUID id;
}
