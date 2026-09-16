package com.glucoze.thesismanagement.user.repository;

import com.glucoze.thesismanagement.user.entity.Admin;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUserAccountUsername(String username);

    @EntityGraph(attributePaths = "userAccount")
    List<Admin> findByUserAccountIdIn(Collection<Long> accountIds);
}
