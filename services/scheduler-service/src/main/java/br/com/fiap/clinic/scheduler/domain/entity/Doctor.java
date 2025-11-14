package br.com.fiap.clinic.scheduler.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "doctors")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("doctor")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Doctor extends User {

    @Column(nullable = false, unique = true)
    private String crm;

    @Column(nullable = false)
    private String specialty;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
