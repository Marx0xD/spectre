package com.spectrun.spectrum.DTO;

import lombok.Data;

@Data
public class ProbeHostResponse {
    private String osFamily;
    private String osVersion;
    private String arch;
}
