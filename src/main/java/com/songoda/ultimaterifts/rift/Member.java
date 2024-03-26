package com.songoda.ultimaterifts.rift;

import com.craftaro.core.data.SQLDelete;
import com.craftaro.core.data.SQLInsert;
import com.craftaro.core.data.SavesData;
import com.craftaro.third_party.org.jooq.DSLContext;

import java.util.UUID;

public class Member implements SavesData {
    private final UUID playerId;
    private final Rift rift;
    private long timestamp;
    private boolean isOwner;

    public Member(UUID playerId, Rift rift, long timestamp) {
        this.playerId = playerId;
        this.rift = rift;
        this.timestamp = timestamp;
        this.isOwner = false;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    @Override
    public void saveImpl(DSLContext dslContext, String... columns) {
        SQLInsert.create(dslContext).insertInto("rift_members")
                .withField("rift_id", rift.getRiftId())
                .withField("player_id", playerId.toString())
                .withField("is_owner", isOwner)
                .withField("timestamp", timestamp)
                .onDuplicateKeyUpdate(columns)
                .execute();
    }

    @Override
    public void deleteImpl(DSLContext dslContext) {
        SQLDelete.create(dslContext).delete("rift_members", "player_id", playerId.toString());
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}