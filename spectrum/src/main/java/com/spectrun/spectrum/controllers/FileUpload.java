package com.spectrun.spectrum.controllers;

import com.spectrun.spectrum.DTO.ApiResponse;
import com.spectrun.spectrum.DTO.ModuleDto;
import com.spectrun.spectrum.services.Implementations.ModuleService;
import com.spectrun.spectrum.services.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;


@RestController
@RequestMapping("/api/v1/module")
public class FileUpload {
private final StorageService storageService;
private final ModuleService moduleService;
@Autowired
    public FileUpload(StorageService storageService, ModuleService moduleService) {
        this.storageService = storageService;
        this.moduleService = moduleService;
    }


    @PostMapping("/new")
    ResponseEntity<ApiResponse<String>> uploadModule(@RequestParam("file") MultipartFile file
                                   ){
        String filePath = storageService.store(file);
        HashMap<String,String> filePathResponse = new HashMap<String,String>();
        filePathResponse.put("path",filePath);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        "Module file uploaded",
                        filePath
                ));
    }
    @PostMapping("/newModule")
    ResponseEntity<ApiResponse<ModuleDto>>  uploadModule(@RequestBody ModuleDto module){
        ModuleDto newModule = this.moduleService.addNewmodule(module);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Module created",
                        newModule
                ));
    }

}
