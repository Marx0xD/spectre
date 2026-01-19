package com.spectrun.spectrum.DTO;

import lombok.Data;

@Data
public class ProbeRequestResult {
    private boolean reachable;
    private String reason;
    private int ssh_port;
    private String banner;
}
