package com.rikkei.jobs.recruitment;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test-only SMTP boundary: accepts real JavaMail SMTP traffic and records MIME DATA. */
final class TestSmtpServer implements AutoCloseable {
    final ServerSocket listener;
    final CopyOnWriteArrayList<byte[]> messages=new CopyOnWriteArrayList<>();
    TestSmtpServer() {
        try {listener=new ServerSocket(0,50,InetAddress.getLoopbackAddress());}
        catch(IOException ex){throw new UncheckedIOException(ex);}
        Thread.ofVirtual().start(()->{while(!listener.isClosed())try{var socket=listener.accept();Thread.ofVirtual().start(()->receive(socket));}catch(IOException ex){if(!listener.isClosed())throw new UncheckedIOException(ex);}});
    }
    int port(){return listener.getLocalPort();}
    void receive(Socket socket){
        try(socket;var reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.ISO_8859_1));var writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.ISO_8859_1),true)){
            writer.print("220 localhost test SMTP\r\n");writer.flush();String line;
            while((line=reader.readLine())!=null){
                if(line.equalsIgnoreCase("DATA")){
                    writer.print("354 Send message\r\n");writer.flush();var body=new StringBuilder();
                    while((line=reader.readLine())!=null&&!line.equals(".")){if(line.startsWith(".."))line=line.substring(1);body.append(line).append("\r\n");}
                    messages.add(body.toString().getBytes(StandardCharsets.ISO_8859_1));writer.print("250 Accepted\r\n");
                }else if(line.equalsIgnoreCase("QUIT")){writer.print("221 Bye\r\n");writer.flush();break;}
                else writer.print("250 OK\r\n");writer.flush();
            }
        }catch(IOException ex){throw new UncheckedIOException(ex);}
    }
    public void close()throws IOException{listener.close();}
}
