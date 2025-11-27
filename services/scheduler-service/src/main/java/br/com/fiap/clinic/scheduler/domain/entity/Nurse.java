package br.com.fiap.clinic.scheduler.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nurses")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("nurse")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Nurse extends User {

    @Column(name = "is_active")
    private Boolean isActive = true;
}