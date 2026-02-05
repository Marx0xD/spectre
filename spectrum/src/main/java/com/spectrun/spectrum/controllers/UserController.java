package com.spectrun.spectrum.controllers;

import com.spectrun.spectrum.DTO.ApiResponse;
import com.spectrun.spectrum.DTO.UserDTO;
import com.spectrun.spectrum.services.Implementations.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log =
            LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping
    public ApiResponse<List<UserDTO>> getAllUsers() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        log.info("Fetching users | requested by={}",
                authentication != null ? authentication.getName() : "anonymous");

        List<UserDTO> users = userService.getUsers();

        return ApiResponse.success(
                "Users fetched",
                users
        );
    }
    @GetMapping("/current")
    public ApiResponse<UserDTO> getCurrentLoggedInUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        // service to fetch user by username and return userdto
        if(email == null || email.isEmpty()){
            return ApiResponse.error("User Not Found") ;
        }
        UserDTO loggedInUser = this.userService.getUserByEmail(email);
        if(loggedInUser == null){

            return ApiResponse.error(
                    "User Not Found") ;
        }

      return ApiResponse.success(
                "User fetched",
                loggedInUser) ;
    }
}
