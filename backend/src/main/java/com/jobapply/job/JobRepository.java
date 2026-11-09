package com.jobapply.job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface JobRepository extends JpaRepository<Job,Long> {
 @Query("select j from Job j where j.status='ACTIVE' and (j.owner is null or j.owner.email=:email) order by j.createdAt desc")
 List<Job> findVisibleToUser(@Param("email") String email);
 List<Job> findByStatusAndOwnerIsNullOrderByCreatedAtDesc(String status);
 @Query("select j from Job j where j.id=:id and j.owner is null")
 Optional<Job> findPublicById(@Param("id") Long id);
 @Query("select j from Job j where j.id=:id and (j.owner is null or j.owner.email=:email)")
 Optional<Job> findVisibleById(@Param("id") Long id,@Param("email") String email);
}
