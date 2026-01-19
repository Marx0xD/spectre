package com.spectrun.spectrum.DTO;

import lombok.Data;

@Data
public class HostDetailRequest {
    private String hostname;     // user label
    private String ipAddress;
    private String sshUsername;
    private String sshPassword;
}
