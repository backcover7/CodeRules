package com.saucer.sast.lang.java.parser.nodes;

import com.contrastsecurity.sarif.ThreadFlow;
import com.contrastsecurity.sarif.ThreadFlowLocation;
import com.saucer.sast.utils.CharUtils;

import java.util.List;

public class CallGraphNode {
    private int MethodID;
    private int InvocationID;
    private int InvocationSourceID;
    private int InvocationTargetID;
    private ThreadFlow intraflow;

    public static String METHODID = "MethodID";
    public static String INVOCATIONID = "InvocationID";
    public static String INVOCATIONSOURCEID = "InvocationSourceID";
    public static String INVOCATIONTARGETID = "InvocationTargetID";
    public static String INTRAFLOW = "intraflow";

    public int getMethodID() {
        return MethodID;
    }

    public void setMethodID(int methodID) {
        MethodID = methodID;
    }

    public int getInvocationID() {
        return InvocationID;
    }

    public void setInvocationID(int invocationID) {
        InvocationID = invocationID;
    }

    public int getInvocationSourceID() {
        return InvocationSourceID;
    }

    public void setInvocationSourceID(int invocationSourceID) {
        InvocationSourceID = invocationSourceID;
    }

    public int getInvocationTargetID() {
        return InvocationTargetID;
    }

    public void setInvocationTargetID(int invocationTargetID) {
        InvocationTargetID = invocationTargetID;
    }

    public ThreadFlow getIntraflow() {
        return intraflow;
    }

    public void setIntraflow(ThreadFlow intraflow) {
        if (intraflow != null) {
            List<ThreadFlowLocation> threadFlowLocations = intraflow.getLocations();
            threadFlowLocations.parallelStream().forEach(this::modifyThreadFlowLocationMessage);
        }
        this.intraflow = intraflow;
    }

    private void modifyThreadFlowLocationMessage(ThreadFlowLocation threadFlowLocation) {
        String text = threadFlowLocation.getLocation().getMessage().getText();
        text = text.replaceAll(" @ '.*'\\Z", CharUtils.empty);
        threadFlowLocation.getLocation().getMessage().setText(text);
        threadFlowLocation.getLocation().getPhysicalLocation().getRegion().getMessage().setText(text);
    }
}
