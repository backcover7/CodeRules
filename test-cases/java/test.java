package com.saucer.sast.utils;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.io.IOException;
import java.io.Serializable;

public class test implements Serializable {
    @DeleteMapping
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("hello");
        target s = new target();
        s.rce(user); //5
    }

    public void finalize() {
        System.out.println("gadgetsource"); //4
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
        String data = intermediate(payload); // 2
        Object anything = "A".toUpperCase(java.util.Locale.ROOT);
        Runtime.getRuntime().exec(data); //3
    }

    public void getGadget(String data) throws IOException {
        String a = intermediate(data);
        new ProcessBuilder(a).start(); //1
    }

    public void node(String b) throws NamingException {
        String a = "a";
        new javax.naming.InitialContext().lookup(a);
    }
}