package com.jobapply.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatusAndOwnerIsNullOrderByCreatedAtDesc(String status);

    @Query("select j from Job j where j.status='ACTIVE' and j.owner.email=:email order by j.createdAt desc")
    List<Job> findOwnedActiveByEmail(@Param("email") String email);

    Optional<Job> findBySourceAndExternalJobId(String source, String externalJobId);

    @Query("select j from Job j where j.id=:id and j.owner is null")
    Optional<Job> findPublicById(@Param("id") Long id);

    @Query("select j from Job j where j.id=:id and j.owner.email=:email")
    Optional<Job> findOwnedByIdAndEmail(@Param("id") Long id, @Param("email") String email);

    /**
     * Compatibility method used by ATS, tailoring and application flows.
     * Use separate queries so shared rows with a null owner are never lost
     * through a combined owner/email OR expression.
     */
    default List<Job> findVisibleToUser(String email) {
        List<Job> visible = new ArrayList<>(
                findByStatusAndOwnerIsNullOrderByCreatedAtDesc("ACTIVE")
        );
        visible.addAll(findOwnedActiveByEmail(email));
        visible.sort(Comparator.comparing(Job::getCreatedAt).reversed());
        return visible;
    }

    default Optional<Job> findVisibleById(Long id, String email) {
        return findPublicById(id).or(() -> findOwnedByIdAndEmail(id, email));
    }
}
