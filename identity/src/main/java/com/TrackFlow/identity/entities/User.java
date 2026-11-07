package com.TrackFlow.identity.entities;

import com.TrackFlow.identity.entities.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 30)
    @Column(name = "first_name", length = 30, nullable = false)
    private String firstName;

    @NotBlank
    @Size(max = 30)
    @Column(name = "last_name", length = 30, nullable = false)
    private String lastName;

    @NotBlank
    @Size(max = 20)
    @Column(unique = true, nullable = false, length = 20)
    private String cin;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$")
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Past
    private LocalDate birthday;

    @Size(max = 100)
    private String address;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private boolean enabled = true;

    @OneToOne(mappedBy = "user")
    private Customer customer;

}
