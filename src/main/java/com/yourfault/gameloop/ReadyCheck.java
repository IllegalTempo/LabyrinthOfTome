package com.yourfault.gameloop;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReadyCheck {
    public enum Mode {
        YES_NO,
        CONFIRM_ONLY
    }

    public enum Status {
        PENDING,
        READY,
        DECLINED
    }

    private final Mode mode;
    private final Map<UUID, Status> responses = new HashMap<>();
    private int readyCount = 0;

    public ReadyCheck(Collection<UUID> participants, Mode mode) {
        this.mode = mode;
        for (UUID id : participants) {
            responses.put(id, Status.PENDING);
        }
        this.readyCount = 0;
    }

    public Mode mode() {
        return mode;
    }

    public boolean contains(UUID id) {
        return responses.containsKey(id);
    }

    public Status status(UUID id) {
        return responses.getOrDefault(id, Status.PENDING);
    }

    public boolean markReady(UUID id) {
        if (!responses.containsKey(id)) {
            return false;
        }
        Status previous = responses.put(id, Status.READY);
        if (previous == Status.READY) {
            return false;
        }
        if (previous != Status.READY) {
            readyCount++;
        }
        return true;
    }

    public boolean markDeclined(UUID id) {
        if (mode != Mode.YES_NO) {
            return false;
        }
        if (!responses.containsKey(id)) {
            return false;
        }
        Status previous = responses.put(id, Status.DECLINED);
        if (previous == Status.READY) {
            readyCount = Math.max(0, readyCount - 1);
        }
        return previous != Status.DECLINED;
    }

    public int totalParticipants() {
        return responses.size();
    }

    public int readyCount() {
        return readyCount;
    }

    public boolean isComplete() {
        return !responses.isEmpty() && readyCount >= responses.size();
    }


    public java.util.Set<UUID> participants() {
        return java.util.Collections.unmodifiableSet(responses.keySet());
    }
}
