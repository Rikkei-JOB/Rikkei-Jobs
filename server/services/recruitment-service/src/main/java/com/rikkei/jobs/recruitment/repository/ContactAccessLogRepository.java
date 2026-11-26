package com.rikkei.jobs.recruitment.repository;
import com.rikkei.jobs.recruitment.entity.ContactAccessLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface ContactAccessLogRepository extends JpaRepository<ContactAccessLog,UUID>, JpaSpecificationExecutor<ContactAccessLog> {}
