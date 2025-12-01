package com.spectrun.spectrum.controllers;


import com.spectrun.spectrum.Enums.JobStatus;
import com.spectrun.spectrum.models.Jobs;
import com.spectrun.spectrum.services.Implementations.CallbackHs256Verifier;
import com.spectrun.spectrum.services.Implementations.JobsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.spectrun.spectrum.DTO.JobUpdateIn;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class jobsController {
    JobsService jobsService;
    CallbackHs256Verifier callbackHs256Verifier;

    public jobsController(JobsService jobsService,CallbackHs256Verifier callbackHs256Verifier)
    {
        this.callbackHs256Verifier = callbackHs256Verifier;
        this.jobsService = jobsService;
    }

    @PostMapping("/{jobId}/callback")
    public ResponseEntity<?> callback (@PathVariable long jobId,
                                       @RequestHeader("X-Callback-Token") String token,
                                       @RequestHeader("X-Idempotency-Key") String idem,
                                       @RequestBody JobUpdateIn in)
    {
        if(token == null){
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            pd.setTitle("Missing callback token");
            pd.setDetail("Header 'X-Callback-Token' is required.");
            pd.setProperty("code", "CALLBACK_TOKEN_REQUIRED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
        }
        // TODO: verify token here; if invalid -> 403 Forbidden
        // if (!jobsService.verifyCallbackToken(jobId, token)) { ... 403 ... }

        // 2) Guard: missing/blank idempotency key -> 400
        if(idem == null){
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            pd.setTitle("Missing idempotency key");
            pd.setDetail("Header 'X-Idempotency-Key' is required to de-duplicate callbacks.");
            pd.setProperty("code", "IDEMPOTENCY_KEY_REQUIRED");
            return ResponseEntity.badRequest().body(pd);
        }
        Jobs job = this.jobsService.getJobByIdemKy(idem);
        //todo:verify token

        if(job == null){
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            pd.setTitle("Job Does Not exists or not created");
            pd.setDetail("The Requested Job Does Not exist.");
            pd.setProperty("code", "JOB_NOT_FOUND");
            return ResponseEntity.status(404).body(pd);
        }
        //callbackHs256Verifier.verifyClaims(token,job.getId(),idem);
        switch (in.getStatus()){
            case PENDING -> {
                if(job.getStatus() == JobStatus.PENDING ){
                    //do nothing, job might have failed for some reason
                    this.jobsService.markJobAsFailed(job.getId(),in.getMessage());
                    return  ResponseEntity.status(408).body(job);
                }
                break;
            }
            case SUCCEEDED -> {
                this.jobsService.markSucceeded(job.getId(),in.getMessage());
                return  ResponseEntity.status(200).body(job);
            }
            case FAILED -> {
                this.jobsService.markJobAsFailed(job.getId(),in.getMessage());
                return  ResponseEntity.status(500).body(job);
            }
            case RUNNING -> {
                this.jobsService.markRunning(job.getId());
                return  ResponseEntity.status(200).body(job);
            }
            default -> {
                ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
                pd.setTitle("Unsupported status");
                pd.setDetail("The provided status is not recognized for this transition.");
                pd.setProperty("code", "INVALID_STATUS");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
            }
        }
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Something Went Wrong");
        pd.setDetail("Unknown Error");
        pd.setProperty("code", "UNKNOWN_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

}
