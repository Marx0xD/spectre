package com.spectrun.spectrum.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spectrun.spectrum.DTO.*;
import com.spectrun.spectrum.Enums.HostInitStatus;
import com.spectrun.spectrum.models.Host;
import com.spectrun.spectrum.services.Implementations.HostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
    public ResponseEntity<?> probeHost(@RequestBody HostDetailRequest probeData) {

        String url = ""; // python probe endpoint

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
                ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
                pd.setTitle("Host probe failed");
                pd.setDetail(response.body());
                pd.setProperty("code", "PROBE_FAILED");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
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

            HostDto dto = new HostDto();
            dto.setId(saved.getId());
            dto.setHostname(saved.getHostname());
            dto.setHostIp(saved.getHostIp());
            dto.setOsFamily(saved.getOsFamily());
            dto.setOsVersion(saved.getOsVersion());
            dto.setArchitecture(saved.getArchitecture());
            dto.setStatus(saved.getStatus());
            dto.setRegisteredAt(saved.getRegisteredAt());
            dto.setUpdatedOn(saved.getUpdatedOn());

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            pd.setTitle("Probe interrupted");
            pd.setDetail("Probe operation was interrupted");
            pd.setProperty("code", "PROBE_INTERRUPTED");

            return ResponseEntity.internalServerError().body(pd);

        } catch (IOException e) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
            pd.setTitle("Probe communication error");
            pd.setDetail(e.getMessage());
            pd.setProperty("code", "PROBE_IO_ERROR");

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
        }
    }

    @PostMapping("/")
    public ResponseEntity<?> probeHost(@RequestBody ProbeHostRequest probeData) {

        String url = "http://localhost:8000/proble/ping";

        try {
            String jsonBody = objectMapper.writeValueAsString(probeData);
            System.out.println(jsonBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Python service failed → control-plane failure
            if (response.statusCode() != 200) {
                ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
                pd.setTitle("Host probe backend error");
                pd.setDetail(response.body());
                pd.setProperty("code", "PROBE_BACKEND_ERROR");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
            }

            // Python responded correctly → deserialize probe result
            ProbeRequestResult result =
                    objectMapper.readValue(response.body(), ProbeRequestResult.class);


            return ResponseEntity.ok(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
            pd.setTitle("Probe interrupted");
            pd.setDetail("Probe operation was interrupted");
            pd.setProperty("code", "PROBE_INTERRUPTED");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);

        } catch (IOException e) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
            pd.setTitle("Probe communication failure");
            pd.setDetail(e.getMessage());
            pd.setProperty("code", "PROBE_IO_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
        }
    }
}
