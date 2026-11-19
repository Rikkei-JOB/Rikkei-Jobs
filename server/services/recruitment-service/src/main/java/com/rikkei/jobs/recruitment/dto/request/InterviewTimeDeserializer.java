package com.rikkei.jobs.recruitment.dto.request;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.*;
/** Only this API requires textual ISO time; do not change shared Jackson settings. */
public class InterviewTimeDeserializer extends JsonDeserializer<OffsetDateTime> {
    @Override public OffsetDateTime deserialize(JsonParser parser,DeserializationContext context)throws IOException {
        if(parser.currentToken()!=JsonToken.VALUE_STRING) throw context.wrongTokenException(parser,OffsetDateTime.class,JsonToken.VALUE_STRING,"Cần thời gian ISO8601 có múi giờ.");
        String value=parser.getText();
        try{return OffsetDateTime.parse(value,DateTimeFormatter.ISO_OFFSET_DATE_TIME);}
        catch(DateTimeParseException ex){throw context.weirdStringException(value,OffsetDateTime.class,"Cần thời gian ISO8601 có offset, ví dụ 2030-01-01T09:00:00+07:00.");}
    }
}
