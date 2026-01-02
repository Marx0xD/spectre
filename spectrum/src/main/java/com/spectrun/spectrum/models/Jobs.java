package com.spectrun.spectrum.models;


import com.spectrun.spectrum.Enums.JobStatus;
import com.spectrun.spectrum.Enums.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jobs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, unique = true, length = 256)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status;
    @Column(columnDefinition = "text")
    private  String message;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    private JobType type;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    // Optional, handy for tracing:
    private String callbackUrl;
    private String correlationKey;

}
