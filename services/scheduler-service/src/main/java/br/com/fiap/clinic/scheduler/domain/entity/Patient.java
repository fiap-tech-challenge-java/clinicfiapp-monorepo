package br.com.fiap.clinic.scheduler.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("patient")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends User {

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
