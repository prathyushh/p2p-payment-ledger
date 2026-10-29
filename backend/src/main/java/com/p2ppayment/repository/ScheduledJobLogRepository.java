package com.p2ppayment.repository;

import com.p2ppayment.entity.ScheduledJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledJobLogRepository extends JpaRepository<ScheduledJobLog, Long> {
}