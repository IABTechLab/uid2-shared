package com.uid2.shared.model;

public record SaltEntry(
        long id,
        String hashedId,
        long lastUpdated,
        String salt,

        Long refreshFrom, // needs to be nullable until V3 Identity Map is fully rolled out
        String previousSalt,

        KeyMaterial currentKey,
        KeyMaterial previousKey
) {
    @Override
    public String toString() {
        return "SaltEntry{" +
                "id=" + id +
                ", hashedId='" + hashedId + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", refreshFrom=" + refreshFrom +
                ", currentKey=" + currentKey +
                ", previousKey=" + previousKey +
                '}';
    }

    public record KeyMaterial(
            int id,
            String key,
            String salt
    ){
        @Override
        public String toString() {
            return "KeyMaterial{id=%d}".formatted(id);
        }
    }
}
