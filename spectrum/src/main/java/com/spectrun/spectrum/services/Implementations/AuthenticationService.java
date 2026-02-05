package com.spectrun.spectrum.services.Implementations;

import com.spectrun.spectrum.DTO.LoginDto;
import com.spectrun.spectrum.DTO.UserDTO;
import com.spectrun.spectrum.models.Users;
import com.spectrun.spectrum.repositories.UserRepsoitory;
import com.spectrun.spectrum.utils.mappers.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private UserRepsoitory userRepsoitory;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;

    public AuthenticationService(UserRepsoitory userRepsoitory, PasswordEncoder passwordEncoder,AuthenticationManager authenticationManager) {
        this.userRepsoitory = userRepsoitory;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public UserDTO registerUser(UserDTO newUser) {
        Users mappedUser = UserMapper.INSTANCE.userDtoToUser(newUser);
        mappedUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        return  UserMapper.INSTANCE.userToUserDto(userRepsoitory.save(mappedUser));

    }

    public Users authenticate(LoginDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return userRepsoitory.findByEmail(input.getEmail())
                .orElseThrow();
    }
}
