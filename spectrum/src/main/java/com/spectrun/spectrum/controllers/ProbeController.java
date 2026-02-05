package com.spectrun.spectrum.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spectrun.spectrum.DTO.*;
import com.spectrun.spectrum.Enums.HostInitStatus;
import com.spectrun.spectrum.models.Host;
import com.spectrun.spectrum.services.Implementations.HostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/api/v1/probe")
public class ProbeController {

    private final HttpClient httpClient;
    private final HostService hostService;
    private final ObjectMapper objectMapper;

    @Value("${app.publicBaseApiUrl}")
    private String orchestratorUrl;

    public ProbeController(
            HttpClient httpClient,
            HostService hostService,
            ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient;
        this.hostService = hostService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/discovery")
    public ResponseEntity<ApiResponse<HostDto>> probeHost(
            @RequestBody HostDetailRequest probeData
    ) {

        String url = orchestratorUrl + "/probe/discovery";

        try {
            String jsonBody = objectMapper.writeValueAsString(probeData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ResponseEntity
                        .status(HttpStatus.BAD_GATEWAY)
                        .body(ApiResponse.error(
                                "Host probe failed"
                        ));
            }

            ProbeHostResponse probeResult =
                    objectMapper.readValue(response.body(), ProbeHostResponse.class);

            Host host = Host.builder()
                    .hostname(probeData.getHostname())
                    .hostIp(probeData.getIpAddress())
                    .sshUsername(probeData.getSshUsername())
                    .sshPassword(probeData.getSshPassword())
                    .osFamily(probeResult.getOsFamily())
                    .osVersion(probeResult.getOsVersion())
                    .architecture(probeResult.getArch())
                    .status(HostInitStatus.PROBED)
                    .build();

            HostDto saved = hostService.createHost(host);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Host probed and registered",
                            saved
                    ));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return ResponseEntity
                    .internalServerError()
                    .body(ApiResponse.error(
                            "Probe operation was interrupted"
                    ));

        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error(
                            "Probe communication error"
                    ));
        }
    }

    @PostMapping("/ping")
    public ResponseEntity<ApiResponse<ProbeRequestResult>> probePing(
            @RequestBody ProbeHostRequest probeData
    ) {

        String url = "http://localhost:8000/probe/ping";

        try {
            String jsonBody = objectMapper.writeValueAsString(probeData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ResponseEntity
                        .status(HttpStatus.BAD_GATEWAY)
                        .body(ApiResponse.error(
                                "Probe backend error"
                        ));
            }

            ProbeRequestResult result =
                    objectMapper.readValue(response.body(), ProbeRequestResult.class);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Probe successful",
                            result
                    )
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error(
                            "Probe operation interrupted"
                    ));

        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error(
                            "Probe communication failure"
                    ));
        }
    }
}
