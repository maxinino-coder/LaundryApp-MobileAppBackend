package com.group130.laundryapp.DAL.Configuration.Auth.AuthSupport;


// ============================================================
//  TokenCapturingResponseWrapper.java
//  HttpServletResponseWrapper that buffers the response body
//  so TokenResponseFilter can read it before it's flushed.
// ============================================================

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.*;
        import java.nio.charset.StandardCharsets;

public class TokenCapturingResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
    private final PrintWriter writer;

    public TokenCapturingResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
        writer = new PrintWriter(new OutputStreamWriter(capturedOutput, StandardCharsets.UTF_8), true);
    }

    @Override
    public PrintWriter getWriter() { return writer; }

    @Override
    public ServletOutputStream getOutputStream() {
        return new ServletOutputStream() {
            @Override public void write(int b) { capturedOutput.write(b); }
            @Override public boolean isReady()  { return true; }
            @Override public void setWriteListener(WriteListener l) {}
        };
    }

    public String getCapturedBody() {
        writer.flush();
        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    public void copyBodyToResponse() throws IOException {
        byte[] body = capturedOutput.toByteArray();
        getResponse().setContentLength(body.length);
        getResponse().getOutputStream().write(body);
    }
}
