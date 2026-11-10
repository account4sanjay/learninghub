package com.learninghub.user.repo;

import java.util.List;
import java.util.Optional;

import com.learninghub.user.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUsername(String username);
    List<Profile> findByRoleIgnoreCaseOrderByLastNameAscFirstNameAsc(String role);
}
