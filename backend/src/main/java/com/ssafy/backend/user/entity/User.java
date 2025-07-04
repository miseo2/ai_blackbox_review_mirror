package com.ssafy.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;    // 또는 import lombok.* (버전에 따라 jakarta)
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "providerId"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(nullable = true)
    private String email;

    @Column(nullable = false)
    private String provider; // 지금은 카카오만

    @Column(nullable = false)
    private String providerId; // 카카오 user id

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt; //생성 일자


}

