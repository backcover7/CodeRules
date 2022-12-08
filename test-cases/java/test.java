package com.saucer.sast.utils;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class test {
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("hello");
        Runtime.getRuntime().exec(user);
        target s = new target();
        s.rce(user);
        System.out.println(user);
    }

    public void finalize() {
        System.out.println("gadgetsource");
    }
}

class target {
    public void branch() {
        new test().finalize();
        System.out.println("x");
    }

    public String intermediate(String data) {
        return data.toLowerCase();
    }

    public void rce(String payload) throws IOException {
        branch();
        String data = intermediate(payload);
        Object anything = "A".toUpperCase(java.util.Locale.ROOT);
        Runtime.getRuntime().exec(data);
    }

    public void gadget(String data) throws IOException {
        new ProcessBuilder(data).start();
    }

    public void node(String b) throws NamingException {
        String a = "a";
        new javax.naming.InitialContext().lookup(a);
    }
}