package com.spectrun.spectrum.controllers;

import com.spectrun.spectrum.DTO.HostDto;
import com.spectrun.spectrum.MessageTemplate.initializeServerTemplate;
import com.spectrun.spectrum.models.Host;
import com.spectrun.spectrum.models.Jobs;
import com.spectrun.spectrum.services.Implementations.CallBackTokenIssuer;
import com.spectrun.spectrum.services.Implementations.CallBackTokenIssuer.IssuedToken;
import com.spectrun.spectrum.services.Implementations.HostService;
import com.spectrun.spectrum.services.Implementations.JobsService;
import com.spectrun.spectrum.utils.mappers.HostMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.spectrun.spectrum.DTO.InitEnqueuedResponse;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/host")
public class host {
    private HostService hostService;
    CallBackTokenIssuer tokenIssuer;
    private KafkaTemplate<String, initializeServerTemplate> intializeServer;
    private JobsService jobsService;
    public HostDto hostToHostDto(Host host){
        return HostMapper.HOST_MAPPER.hostToHostDto(host);
    }

    public host(HostService hostService,KafkaTemplate<String, initializeServerTemplate> intializeServer,JobsService jobsService,CallBackTokenIssuer tokenIssuer) {
     this.hostService = hostService;
     this.intializeServer = intializeServer;
     this.jobsService = jobsService;
     this.tokenIssuer = tokenIssuer;
    }

    @PostMapping
    public ResponseEntity<?> validateAndQueue(@RequestBody Host host, UriComponentsBuilder uri){

        //validate this and create a host
        if(this.hostService.checkIfHostExists(host.getHostname())){
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
            pd.setTitle("Hostname already exists");
            pd.setDetail("A host with hostname '%s' already exists.".formatted(host.getHostname()));
            pd.setProperty("code", "HOSTNAME_EXISTS");
            pd.setProperty("field", "hostname");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
        }

        Jobs job = this.jobsService.CreatePendingJob("init-host:" + host.getHostname());
        IssuedToken jobToken = tokenIssuer.issueForJob(job.getId(),Duration.from(ChronoUnit.HOURS.getDuration()),job.getIdempotencyKey());
        initializeServerTemplate template = new initializeServerTemplate(host.getHostname(),host.getSshUsername(),host.getSshPassword(),jobToken.token(),job.getCallbackUrl(),job.getId(),job.getIdempotencyKey());
        this.intializeServer.send("initialize-server",template);

        return ResponseEntity.accepted()
                .location(uri.path("/hosts/jobs/{id}").buildAndExpand(job.getId()).toUri())
                .body(new InitEnqueuedResponse(job.getId(), "PENDING"));
    }
    @GetMapping("/host/{id}")
    public ResponseEntity<HostDto> getHostById(@PathVariable long id)
    {
        HostDto hostInformation = this.hostService.findHostById(id).orElse(null);
        if(hostInformation == null){
            return  new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        return  new ResponseEntity<>(hostInformation, HttpStatus.OK);
    }

    @PutMapping("/host/{id}")
    public ResponseEntity<HostDto> updateHost(@PathVariable long hostId,Host host){
        HostDto updatedHost = this.hostService.updateHost(hostId,host);
        return new ResponseEntity<>(updatedHost, HttpStatus.OK);
    }


    @GetMapping("/initialize-server/{id}")
    public ResponseEntity<?> intializeServer(@PathVariable long id){
        HostDto host = this.hostService.findHostById(id).orElse(null);
        if (host != null){
            initializeServerTemplate template = new initializeServerTemplate();
            template.setHost(host.getHostname());
            template.setUsername(host.getSshUsername());
            template.setPassword(host.getSshPassword());


            this.intializeServer.send("initialize-server",template);
        }
        return  new ResponseEntity<>("data",HttpStatus.OK);
    }
    //post mapping to create  the host once it has been activiated,
    @PostMapping("/new")
    public ResponseEntity<Object> createNewHost(@RequestBody Host host){
        HostDto newHost = this.hostService.createHost(host);

        return  ResponseEntity.status(HttpStatus.CREATED).body(newHost);
    }

}
