package com.jobapply.application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ApplicationRepository extends JpaRepository<ApplicationRecord,Long> {
 List<ApplicationRecord> findByUserEmailOrderByCreatedAtDesc(String email);
 Optional<ApplicationRecord> findByIdAndUserEmail(Long id,String email);
 boolean existsByUserEmailAndJobId(String email,Long jobId);
 long countByUserEmail(String email);
 long countByUserEmailAndStatusIn(String email,List<String> statuses);
}
