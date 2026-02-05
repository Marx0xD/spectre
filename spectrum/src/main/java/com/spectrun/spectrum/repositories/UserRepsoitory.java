package com.spectrun.spectrum.repositories;

import com.spectrun.spectrum.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepsoitory extends JpaRepository<Users,Long> {
    Optional<Users> findByEmail(String email);

}
