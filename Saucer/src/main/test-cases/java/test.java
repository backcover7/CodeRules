package com.saucer.sast.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class test {
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("hello");
        target s = new target();
        s.rce(user);
    }
}

class target {
    public String intermediate(String data) {
        return data.toLowerCase();
    }

    public void rce(String payload) throws IOException {
        Runtime.getRuntime().exec(intermediate(payload));
    }
}