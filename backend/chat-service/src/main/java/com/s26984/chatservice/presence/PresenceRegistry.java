package com.s26984.chatservice.presence;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceRegistry {

    // roomId
    private final Map<String, Set<String>> roomToUsers = new ConcurrentHashMap<>();
    // sessionId
    private final Map<String, Set<String>> userToRooms = new ConcurrentHashMap<>();

    public Set<String> join(String roomId, String sessionId) {
        roomToUsers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        userToRooms.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(roomId);
        return Collections.unmodifiableSet(roomToUsers.get(roomId));
    }

    public Set<String> leave(String roomId, String sessionId) {
        roomToUsers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).remove(sessionId);
        userToRooms.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).remove(roomId);


        if (roomToUsers.get(roomId) != null && roomToUsers.get(roomId).isEmpty()) {
            roomToUsers.remove(roomId);
        }
        if (userToRooms.get(sessionId) != null && userToRooms.get(sessionId).isEmpty()) {
            userToRooms.remove(sessionId);
        }
        return Collections.unmodifiableSet(roomToUsers.getOrDefault(roomId, Set.of()));
    }

    public Set<String> roomsOf(String sessionId) {
        return Collections.unmodifiableSet(userToRooms.getOrDefault(sessionId, Set.of()));
    }
}
