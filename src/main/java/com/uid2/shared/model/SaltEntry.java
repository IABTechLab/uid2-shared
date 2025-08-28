package com.uid2.shared.model;

public record SaltEntry(
        long id,
        String hashedId,
        long lastUpdated,
        String currentSalt,

        long refreshFrom,
        String previousSalt,

        KeyMaterial currentKeySalt,
        KeyMaterial previousKeySalt
) {
    @Override
    public String toString() {
        return "SaltEntry{" +
                "id=" + id +
                ", hashedId='" + hashedId + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", currentSalt=" + (currentSalt == null ? "null" : "<REDACTED>") +
                ", refreshFrom=" + refreshFrom +
                ", previousSalt=" + (previousSalt == null ? "null" : "<REDACTED>") +
                ", currentKeySalt=" + currentKeySalt +
                ", previousKeySalt=" + previousKeySalt +
                '}';
    }

    public record KeyMaterial(
            int id,
            String key,
            String salt
    ) {
        @Override
        public String toString() {
            return "KeyMaterial{" +
                    "id=" + id +
                    ", key=<REDACTED>" +
                    ", salt=<REDACTED>" +
                    '}';
        }
    }
}
