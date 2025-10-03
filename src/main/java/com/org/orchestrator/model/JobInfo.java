package com.org.orchestrator.model;

public class JobInfo {
    private final long id;
    private final String name;
    private final boolean haveArtifacts;

    public JobInfo(long id, String name, boolean haveArtifacts) {
        this.id = id;
        this.name = name;
        this.haveArtifacts = haveArtifacts;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean getHaveArtifacts() {
        return haveArtifacts;
    }
}
