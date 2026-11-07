package com.TrackFlow.identity.entities;

import com.TrackFlow.identity.entities.enums.IdType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "customers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_type", "id_number"}))
@Data @NoArgsConstructor @AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", nullable = false)
    private IdType idType;

    @Column(name = "id_number", nullable = false, length = 30)
    private String idNumber;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
