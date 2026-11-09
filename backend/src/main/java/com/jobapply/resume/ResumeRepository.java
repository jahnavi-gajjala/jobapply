package com.jobapply.resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ResumeRepository extends JpaRepository<Resume,Long> {
 List<Resume> findByUserEmailOrderByCreatedAtDesc(String email);
 Optional<Resume> findByIdAndUserEmail(Long id,String email);
}
