package com.spectrun.spectrum.controllers;

import com.spectrun.spectrum.DTO.ApiResponse;
import com.spectrun.spectrum.DTO.JobUpdateIn;
import com.spectrun.spectrum.Enums.JobStatus;
import com.spectrun.spectrum.models.Jobs;
import com.spectrun.spectrum.services.Implementations.CallbackHs256Verifier;
import com.spectrun.spectrum.services.Implementations.JobsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobsController {

    private final JobsService jobsService;
    private final CallbackHs256Verifier callbackVerifier;

    public JobsController(
            JobsService jobsService,
            CallbackHs256Verifier callbackHs256Verifier
    ) {
        this.jobsService = jobsService;
        this.callbackVerifier = callbackHs256Verifier;
    }

    @PostMapping("/{jobId}/callback")
    public ResponseEntity<ApiResponse<Jobs>> callback(
            @PathVariable long jobId,
            @RequestHeader(value = "X-Callback-Token", required = false) String token,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idem,
            @RequestBody JobUpdateIn in
    ) {

        // 1️⃣ Required headers
        if (token == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Callback token is required"));
        }

        if (idem == null) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("Idempotency key is required"));
        }

        // 2️⃣ Job lookup
        Jobs job = jobsService.getJobByIdemKy(idem);

        if (job == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Job not found"));
        }

        // 3️⃣ Token verification
        callbackVerifier.verifyClaims(token, job.getId(), idem);

        // 4️⃣ State transition
        switch (in.getStatus()) {

            case PENDING -> {
                if (job.getStatus() == JobStatus.PENDING) {
                    jobsService.markJobAsFailed(job.getId(), in.getMessage());
                    return ResponseEntity
                            .status(HttpStatus.REQUEST_TIMEOUT)
                            .body(ApiResponse.error(
                                    "Job remained pending and was marked as failed"
                            ));
                }
            }

            case RUNNING -> {
                jobsService.markRunning(job.getId());
                return ResponseEntity.ok(
                        ApiResponse.success(
                                "Job marked as running",
                                job
                        )
                );
            }

            case SUCCEEDED -> {
                jobsService.markSucceeded(job.getId(), in.getMessage());
                return ResponseEntity.ok(
                        ApiResponse.success(
                                "Job completed successfully",
                                job
                        )
                );
            }

            case FAILED -> {
                jobsService.markJobAsFailed(job.getId(), in.getMessage());
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error(
                                "Job execution failed"
                        ));
            }

            default -> {
                return ResponseEntity
                        .unprocessableEntity()
                        .body(ApiResponse.error(
                                "Unsupported job status transition"
                        ));
            }
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unhandled job callback state"));
    }
}
