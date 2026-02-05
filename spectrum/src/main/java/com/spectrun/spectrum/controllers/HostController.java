package com.spectrun.spectrum.controllers;

import com.spectrun.spectrum.DTO.ApiResponse;
import com.spectrun.spectrum.DTO.HostDto;
import com.spectrun.spectrum.DTO.InitEnqueuedResponse;
import com.spectrun.spectrum.MessageTemplate.initializeServerTemplate;
import com.spectrun.spectrum.models.Host;
import com.spectrun.spectrum.models.Jobs;
import com.spectrun.spectrum.services.Implementations.CallBackTokenIssuer;
import com.spectrun.spectrum.services.Implementations.CallBackTokenIssuer.IssuedToken;
import com.spectrun.spectrum.services.Implementations.HostService;
import com.spectrun.spectrum.services.Implementations.JobsService;
import com.spectrun.spectrum.services.Implementations.SymetricEncryptionService;
import com.spectrun.spectrum.utils.mappers.HostMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/host")
public class HostController {

    private final HostService hostService;
    private final SymetricEncryptionService encryptionService;
    private final KafkaTemplate<String, initializeServerTemplate> initializeServer;
    private final JobsService jobsService;
    private final CallBackTokenIssuer tokenIssuer;

    public HostController(
            HostService hostService,
            SymetricEncryptionService encryptionService,
            KafkaTemplate<String, initializeServerTemplate> initializeServer,
            JobsService jobsService,
            CallBackTokenIssuer tokenIssuer
    ) {
        this.hostService = hostService;
        this.encryptionService = encryptionService;
        this.initializeServer = initializeServer;
        this.jobsService = jobsService;
        this.tokenIssuer = tokenIssuer;
    }

    private HostDto toDto(Host host) {
        return HostMapper.HOST_MAPPER.hostToHostDto(host);
    }

    // Validate host + enqueue initialization
    @PostMapping
    public ResponseEntity<ApiResponse<InitEnqueuedResponse>> validateAndQueue(
            @RequestBody Host host,
            UriComponentsBuilder uri
    ) {
        if (hostService.checkIfHostExists(host.getHostname())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "Hostname already exists"
                    ));
        }

        Jobs job = jobsService.CreatePendingJob("init-host:" + host.getHostname());

        String encryptedSsh = encryptionService
                .symetricEncryptService(host.getSshPassword());

        IssuedToken jobToken = tokenIssuer.issueForJob(
                job.getId(),
                Duration.from(ChronoUnit.HOURS.getDuration()),
                job.getIdempotencyKey()
        );

        initializeServerTemplate template =
                new initializeServerTemplate(
                        host.getHostname(),
                        host.getSshUsername(),
                        encryptedSsh,
                        jobToken.token(),
                        job.getCallbackUrl(),
                        job.getId(),
                        job.getIdempotencyKey()
                );

        initializeServer.send("initialize-server", template);

        InitEnqueuedResponse payload =
                new InitEnqueuedResponse(job.getId(), "PENDING");

        return ResponseEntity
                .accepted()
                .location(
                        uri.path("/hosts/jobs/{id}")
                                .buildAndExpand(job.getId())
                                .toUri()
                )
                .body(ApiResponse.success(
                        "Host initialization queued",
                        payload
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HostDto>> getHostById(
            @PathVariable long id
    ) {
        return hostService.findHostById(id)
                .map(host -> ResponseEntity.ok(
                        ApiResponse.success(
                                "Host fetched",
                                host
                        )
                ))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(
                                "Host not found"
                        )));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HostDto>> updateHost(
            @PathVariable long id,
            @RequestBody Host host
    ) {
        HostDto updated = hostService.updateHost(id, host);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Host updated",
                        updated
                )
        );
    }

    // manual trigger (kept, but cleaned)
    @PostMapping("/initialize-server/{id}")
    public ResponseEntity<ApiResponse<Void>> initializeServer(
            @PathVariable long id
    ) {
        HostDto host = hostService.findHostById(id).orElse(null);

        if (host == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(
                            "Host not found"
                    ));
        }

        initializeServerTemplate template = new initializeServerTemplate();
        template.setHost(host.getHostname());
        template.setUsername(host.getSshUsername());
        template.setPassword(host.getSshPassword());

        initializeServer.send("initialize-server", template);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Initialization triggered",
                        null
                )
        );
    }

    @PostMapping("/new")
    public ResponseEntity<ApiResponse<HostDto>> createNewHost(
            @RequestBody Host host
    ) {
        HostDto created = hostService.createHost(host);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Host created",
                        created
                ));
    }
}
