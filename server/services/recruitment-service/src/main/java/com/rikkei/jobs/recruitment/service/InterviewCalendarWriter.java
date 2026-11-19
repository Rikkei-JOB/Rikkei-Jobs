package com.rikkei.jobs.recruitment.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.recruitment.entity.InterviewNotificationOutbox;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class InterviewCalendarWriter {
    private final ObjectMapper json;
    private static final DateTimeFormatter UTC=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    public InterviewNotificationPayload payload(InterviewNotificationOutbox n) {
        try{return json.readValue(n.getPayload(),InterviewNotificationPayload.class);}catch(Exception ex){throw new IllegalStateException(ex);}
    }
    static String escape(String s){return s==null?"":s.replace("\\","\\\\").replace("\r\n","\n").replace("\r","\n").replace("\n","\\n").replace(";","\\;").replace(",","\\,");}
    public byte[] write(InterviewNotificationOutbox n) {
        var p=payload(n);boolean cancel="CANCELLATION".equals(n.getType());
        var lines=new ArrayList<>(List.of("BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//Rikkei Jobs//UT-06//VI","CALSCALE:GREGORIAN","METHOD:"+(cancel?"CANCEL":"REQUEST"),"BEGIN:VEVENT","UID:"+p.scheduleId()+"@rikkei-jobs","SEQUENCE:"+(cancel?1:0),"DTSTAMP:"+UTC.format(n.getCreatedAt()),"DTSTART:"+UTC.format(p.startAt()),"DTEND:"+UTC.format(p.endAt()),"SUMMARY:"+escape("Phỏng vấn "+p.title()+" - "+p.companyName()),"DESCRIPTION:"+escape("Ứng viên: "+p.candidateName()+"\nPhòng họp: "+p.meetUrl()),"URL:"+escape(p.meetUrl()),"ORGANIZER:mailto:"+p.interviewerEmail(),"STATUS:"+(cancel?"CANCELLED":"CONFIRMED")));
        for(String email:List.of(p.candidateEmail(),p.interviewerEmail()).stream().map(InterviewResourceLock::email).distinct().toList()) lines.add("ATTENDEE;ROLE=REQ-PARTICIPANT:mailto:"+email);
        lines.add("END:VEVENT");lines.add("END:VCALENDAR");var result=new StringBuilder();
        for(String line:lines) {
            int bytes=0;
            for(int cp:line.codePoints().toArray()) {String ch=new String(Character.toChars(cp));int len=ch.getBytes(StandardCharsets.UTF_8).length;if(bytes+len>75){result.append("\r\n ");bytes=1;}result.append(ch);bytes+=len;}
            result.append("\r\n");
        }
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }
}
