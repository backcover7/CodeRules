package com.saucer.sast.lang.javascript;

import java.util.HashMap;

public interface VulnScan {
    public void Scan(String codebase) throws Exception;

    public void reportContent(HashMap<String, Object> result) throws Exception;

}
