package com.spectrun.spectrum.MessageTemplate;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class initializeServerTemplate implements Serializable {
    private String host;
    private String username;
    private  String password;
    private String token;
    private String callbackUrl;
    @JsonProperty("JobId")
    private long JobId;
    private String idempotencyKey;


}
