package com.saucer.sast.utils;

import com.contrastsecurity.sarif.*;
import com.saucer.sast.lang.java.parser.nodes.InvocationNode;
import com.saucer.sast.lang.java.parser.nodes.MethodNode;
import spoon.reflect.cu.SourcePosition;

import java.util.HashSet;
import java.util.Set;

public class SpoonUtils {
    public static Location ConvertPosition2Location(MethodNode methodNode, SourcePosition sourcePosition) {
        LogicalLocation logicalLocation = new LogicalLocation();
        logicalLocation.setFullyQualifiedName(methodNode.getSimpleMethodNode().getFullQualifiedName());
        logicalLocation.setName(methodNode.getSimpleMethodNode().getName());
        Set<LogicalLocation> logicalLocations = new HashSet<>();
        logicalLocations.add(logicalLocation);

        PhysicalLocation physicalLocation = new PhysicalLocation();
        physicalLocation.setArtifactLocation(new ArtifactLocation().withUri(sourcePosition.getFile().getAbsolutePath()));
        physicalLocation.setRegion(
                new Region()
                        .withSnippet(new ArtifactContent().withText(methodNode.getSignature()))
                        .withStartLine(sourcePosition.getLine())
                        .withStartColumn(sourcePosition.getColumn()));

        return new Location()
                .withLogicalLocations(logicalLocations)
                .withPhysicalLocation(physicalLocation);
    }

    public static Location ConvertPosition2Location(InvocationNode invocationNode, SourcePosition sourcePosition) {
        LogicalLocation logicalLocation = new LogicalLocation();
        logicalLocation.setFullyQualifiedName(invocationNode.getSourceNode().getMethodNode().getSimpleMethodNode().getFullQualifiedName());
        logicalLocation.setName(invocationNode.getSourceNode().getMethodNode().getSimpleMethodNode().getName());
        Set<LogicalLocation> logicalLocations = new HashSet<>();
        logicalLocations.add(logicalLocation);

        PhysicalLocation physicalLocation = new PhysicalLocation();
        physicalLocation.setArtifactLocation(new ArtifactLocation().withUri(sourcePosition.getFile().getAbsolutePath()));
        physicalLocation.setRegion(
                new Region()
                        .withSnippet(new ArtifactContent().withText(invocationNode.getSnippet()))
                        .withStartLine(sourcePosition.getLine())
                        .withStartColumn(sourcePosition.getColumn()));

        return new Location()
                .withLogicalLocations(logicalLocations)
                .withPhysicalLocation(physicalLocation);
    }
}
