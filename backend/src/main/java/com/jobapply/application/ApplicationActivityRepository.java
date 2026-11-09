package com.jobapply.application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApplicationActivityRepository extends JpaRepository<ApplicationActivity,Long> {
 List<ApplicationActivity> findByApplicationIdAndUserEmailOrderByCreatedAtAsc(Long applicationId,String email);
}
