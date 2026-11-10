package com.rikkei.jobs.identity.repository;

import com.rikkei.jobs.identity.entity.UsedSsoTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsedSsoTicketRepository extends JpaRepository<UsedSsoTicket, String> {
}
