package com.uid2.shared.model;

public record SaltEntry(
        long id,
        String hashedId,
        long lastUpdated,
        String currentSalt,

        Long refreshFrom, // needs to be nullable until V3 Identity Map is fully rolled out
        String previousSalt,

        KeyMaterial currentKey,
        KeyMaterial previousKey
) {
//    @Override
//    public String toString() {
//        return "SaltEntry{" +
//                "id=" + id +
//                ", hashedId='" + hashedId + '\'' +
//                ", lastUpdated=" + lastUpdated +
//                ", currentSalt=<REDACTED>" +
//                ", refreshFrom=" + refreshFrom +
//                ", previousSalt=<REDACTED>" +
//                ", currentKey=" + currentKey +
//                ", previousKey=" + previousKey +
//                '}';
//    }

    public record KeyMaterial(
            int id,
            String key,
            String salt
    ) {
//        @Override
//        public String toString() {
//            return "KeyMaterial{" +
//                    "id=" + id +
//                    ", key=<REDACTED>" +
//                    ", currentSalt=<REDACTED>" +
//                    '}';
//        }
    }
}
