package com.spectrun.spectrum.controllers;

import com.spectrun.spectrum.DTO.ApiResponse;
import com.spectrun.spectrum.DTO.LoginDto;
import com.spectrun.spectrum.DTO.UserDTO;
import com.spectrun.spectrum.models.Users;
import com.spectrun.spectrum.services.Implementations.AuthenticationService;
import com.spectrun.spectrum.services.Implementations.JWTService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/")
public class authentication {
private final JWTService jwtService;
@Value("security.jwt.secret-key")
private String secret;
private final AuthenticationService authenticationService;
    Logger logger = Logger.getLogger(JWTService.class.getName());
    public authentication(JWTService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }
    @PostMapping(value = "/auth/signup",consumes = {"application/json", "application/json;charset=UTF-8"})
    public ResponseEntity<ApiResponse<UserDTO>> register(@RequestBody UserDTO userDTO)
    {
        UserDTO registereduser = authenticationService.registerUser(userDTO);
        ApiResponse<UserDTO> response = ApiResponse.success("user registered",registereduser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> authenticate(@RequestBody LoginDto loginUserDto) {

        Users authenticatedUser = authenticationService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setExpiresIn(jwtService.getExpirationTime());
        loginResponse.setToken(jwtToken);
        ApiResponse<LoginResponse> response = ApiResponse.success("user registered",loginResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    //fast api token request

}
