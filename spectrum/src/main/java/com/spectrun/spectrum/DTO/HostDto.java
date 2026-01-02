package com.spectrun.spectrum.DTO;

import com.spectrun.spectrum.Enums.HostInitStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HostDto {
    private long id;
    private String hostname;
    private String sshUsername;
    private String sshPassword;
    private String HostIp;
    // system facts (confirmed after probe)
    private String osFamily;
    private String osVersion;
    private String architecture;

    // lifecycle
    private HostInitStatus status;

    // timestamps (optional but useful)
    private LocalDateTime registeredAt;
    private LocalDateTime updatedOn;
}
