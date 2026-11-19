package com.rikkei.jobs.recruitment.service.impl;
import com.rikkei.jobs.recruitment.service.*;
import com.rikkei.jobs.recruitment.entity.InterviewNotificationOutbox;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Component;
import jakarta.mail.internet.MimeMessage;
@Component
public class SmtpInterviewMailSender implements InterviewMailSender {
    private final JavaMailSender sender;
    private final InterviewCalendarWriter calendar;
    private final String from,web;
    public SmtpInterviewMailSender(JavaMailSender sender,InterviewCalendarWriter calendar,@Value("${app.recruitment.mail-from}") String from,@Value("${app.recruitment.web-base-url}") String web){this.sender=sender;this.calendar=calendar;this.from=from;this.web=web;}
    public void send(InterviewNotificationOutbox n) {
        try {
            var p=calendar.payload(n);
            MimeMessage msg=new MimeMessage(sender.createMimeMessage().getSession()) {
                @Override protected void updateMessageID() throws jakarta.mail.MessagingException {setHeader("Message-ID","<"+n.getId()+"@rikkei-jobs.local>");}
            };
            var helper=new MimeMessageHelper(msg,true,"UTF-8");helper.setFrom(from);helper.setTo(n.getRecipient());
            String title=Map.of("INVITATION","Mời chọn giờ phỏng vấn","CONFIRMATION","Xác nhận lịch phỏng vấn","CANCELLATION","Huỷ lịch phỏng vấn","REMINDER_24H","Nhắc lịch phỏng vấn trong 24 giờ","REMINDER_1H","Nhắc lịch phỏng vấn trong 1 giờ").get(n.getType());
            helper.setSubject(title+" — "+p.companyName());
            String text=title+"\nVòng: "+p.title()+"\nỨng viên: "+p.candidateName()+"\nNgười phỏng vấn: "+p.interviewerEmail();
            if(p.startAt()!=null) text+="\nThời gian: "+DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(p.startAt())+" (Asia/Ho_Chi_Minh)\nPhòng họp: "+p.meetUrl();
            text+="\nXem lịch: "+web+(n.getRecipient().equalsIgnoreCase(p.candidateEmail())?"/candidate/interviews":"/hr/interviews");
            helper.setText(text,false);
            if("CONFIRMATION".equals(n.getType()) || "CANCELLATION".equals(n.getType())) helper.addAttachment("interview.ics",new ByteArrayResource(calendar.write(n)),"text/calendar; charset=UTF-8; method="+("CANCELLATION".equals(n.getType())?"CANCEL":"REQUEST"));
            sender.send(msg);
        }catch(jakarta.mail.MessagingException ex){throw new MailPreparationException("Không chuẩn bị được email phỏng vấn.",ex);}
    }
}
