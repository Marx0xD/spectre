package com.spectrun.spectrum.models;

import com.spectrun.spectrum.Enums.HostInitStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Host {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @Column(unique = true) //user label could be odoo-instance-addis or sth like that
    private String hostname;
    @Column(unique = true)
    private String hostIp;
    private String sshUsername;
    private String sshPassword;
    private String osFamily;
    private String architecture;
    private String osVersion;
    @Enumerated(EnumType.STRING)
    private HostInitStatus status;
    @CreationTimestamp
    private LocalDateTime registeredAt;
    @UpdateTimestamp
    private LocalDateTime updatedOn;
    //remove this two we do not need them
    private String jobId;
    private String callBackUrl;

}
