package com.saucer.sast.lang.javascript;

public interface VulnScan {
    public void Scan(String codebase) throws Exception;

    public void reportContent() throws Exception;

}
