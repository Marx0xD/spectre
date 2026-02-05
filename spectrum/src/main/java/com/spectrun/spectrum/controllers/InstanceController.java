package com.spectrun.spectrum.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.spectrun.spectrum.DTO.*;
import com.spectrun.spectrum.Enums.Status;
import com.spectrun.spectrum.MessageTemplate.createInstanceTemplate;
import com.spectrun.spectrum.models.Instances;
import com.spectrun.spectrum.models.Subscriptions;
import com.spectrun.spectrum.services.Implementations.InstanceService;
import com.spectrun.spectrum.services.Implementations.ModuleService;
import com.spectrun.spectrum.services.Implementations.UserService;
import com.spectrun.spectrum.utils.API.Request;
import com.spectrun.spectrum.utils.API.RequestDTO.InstallModuleDto;
import com.spectrun.spectrum.utils.API.ResponseDTO.moduleInstallResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

import static com.spectrun.spectrum.utils.URLParser.getPortFromAddress;

@RestController
@RequestMapping("/api/v1/instances")
@Deprecated
public class InstanceController {

    private final InstanceService instanceService;
    private final ModuleService moduleService;
    private final UserService userService;
    private final KafkaTemplate<String, createInstanceTemplate> createInstance;

    private final Logger logger = Logger.getLogger(InstanceController.class.getName());

    public InstanceController(
            ModuleService moduleService,
            InstanceService instanceService,
            KafkaTemplate<String, createInstanceTemplate> createInstanceTemplate,
            UserService userService
    ) {
        this.instanceService = instanceService;
        this.createInstance = createInstanceTemplate;
        this.userService = userService;
        this.moduleService = moduleService;
    }

    // legacy create + enqueue
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<InstanceDto>> createNewInstance(
            @RequestBody InstanceModuleDto installationData
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDTO user = userService.getUserByEmail(auth.getName());

        if (user == null || user.getSubscription() == null) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("No active subscription found"));
        }

        Subscriptions subscription = user.getSubscription();
        long limit = subscription.getUsageLimits().getInstanceLimit();

        long activeInstances = user.getInstances().stream()
                .filter(i -> i.getStatus() == Status.Active)
                .count();

        if (activeInstances >= limit) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(
                            "Instance limit reached for current subscription"
                    ));
        }

        createInstanceTemplate template = new createInstanceTemplate(
                installationData.getInstallationInstance().getInstanceName(),
                installationData.getInstallationModule().getModuleName(),
                installationData.getInstallationModule().getModulePath()
        );

        createInstance.send("create_instance", template);

        InstanceDto instance = InstanceDto.builder()
                .instanceName(installationData.getInstallationInstance().getInstanceName())
                .status(Status.Pending)
                .userId(user.getId())
                .build();

        InstanceDto created = instanceService.createNewInstance(instance);

        return ResponseEntity
                .accepted()
                .body(ApiResponse.success(
                        "Instance creation queued",
                        created
                ));
    }

    @GetMapping
    public ApiResponse<List<InstanceDto>> getAllInstances() {
        return ApiResponse.success(
                "Instances fetched",
                instanceService.getAllInstances()
        );
    }

    @GetMapping("/user/{id}")
    public ApiResponse<List<InstanceDto>> getUserInstances(
            @PathVariable long id
    ) {
        return ApiResponse.success(
                "User instances fetched",
                instanceService.getAllUserInstances(id)
        );
    }

    @PutMapping("/install/{moduleId}/{instanceId}")
    public ResponseEntity<ApiResponse<moduleInstallResponseDTO>> installModule(
            @PathVariable int moduleId,
            @PathVariable int instanceId
    ) throws Exception {

        ModuleDto module = moduleService.getModuleById(moduleId);
        InstanceDto instance = instanceService.getInstanceById(instanceId);

        InstallModuleDto payload = new InstallModuleDto();
        payload.setDb(instance.getInstancedbName());
        payload.setHost("127.0.0.1");
        payload.setUser(instance.getAdminUserName());
        payload.setPassword(instance.getAdminPassword());
        payload.setModule(module.getModuleName());
        payload.setPort(getPortFromAddress(instance.getInstanceaddress()));

        Request<InstallModuleDto, moduleInstallResponseDTO> request = new Request<>();

        moduleInstallResponseDTO response =
                request.handleApiCall(
                        payload,
                        "http://127.0.0.1:5050/api/v1/containerManager/installModule",
                        new TypeReference<
                                com.spectrun.spectrum.utils.API.ResponseBody.ResponseBody<
                                        moduleInstallResponseDTO>>() {}
                ).getData();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Module installation triggered",
                        response
                )
        );
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<InstanceDto>> getByStatus(
            @PathVariable Status status
    ) {
        return ApiResponse.success(
                "Instances fetched by status",
                instanceService.getInstanceByStatus(status)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteInstance(
            @PathVariable long id
    ) {
        return ApiResponse.success(
                "Instance deleted",
                instanceService.deleteInstanceById(id)
        );
    }
}
