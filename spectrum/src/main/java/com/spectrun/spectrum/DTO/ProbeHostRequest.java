package com.spectrun.spectrum.DTO;

import lombok.Data;

@Data
public class ProbeHostRequest {
    private String host;     // user label
    private String ssh_port;

}
