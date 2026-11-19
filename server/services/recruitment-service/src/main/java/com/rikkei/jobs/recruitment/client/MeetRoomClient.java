package com.rikkei.jobs.recruitment.client;
import java.time.Instant;
import java.util.UUID;
public interface MeetRoomClient { String create(UUID slotId,String title,Instant startAt,Instant endAt); }
