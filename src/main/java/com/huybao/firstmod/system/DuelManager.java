package com.huybao.firstmod.system;

import com.huybao.firstmod.data.PlayerChampionData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// Multiplayer duel system. PvP is off globally; the only way two players can hurt
// each other is to be in an active duel together.
//   - challenge() stores a pending request that expires after 30s
//   - accept() turns it into a live duel; deny() drops it
//   - whoever dies first loses: the winner gets champion XP and the duel ends
public final class DuelManager {

    public static final int REQUEST_EXPIRY_TICKS = 30 * 20; // 30 seconds

    // Win reward scales with the level gap: beat someone above you and you earn more,
    // farm someone below you and you earn less (but never nothing).
    public static final int BASE_DUEL_XP = 1000;
    private static final int XP_PER_LEVEL_GAP = 100; // per level of difference
    private static final int MIN_DUEL_XP = 100;

    // target UUID -> who challenged them (only the newest request per target is kept)
    private static final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();
    // both directions are stored so lookups from either side are O(1)
    private static final Map<UUID, UUID> activeDuels = new HashMap<>();

    private record PendingRequest(UUID challenger, String challengerName, int expiryTick) {
    }

    private DuelManager() {
    }

    public static void register() {
        // Drop expired requests once a second.
        ServerTickEvents.END_SERVER_TICK.register(DuelManager::expireRequests);

        // PvP off unless the two players are dueling each other.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity victim)) {
                return true; // not a player getting hit — leave it alone
            }
            Entity attacker = source.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity)) {
                return true; // PvE, environment, etc. are unaffected
            }
            return areDueling(victim.getUuid(), attacker.getUuid());
        });

        // First to die loses the duel.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity loser) {
                onPlayerDeath(loser);
            }
        });

        // Leaving mid-duel forfeits and clears any pending requests.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                onPlayerLeave(handler.player, server));
    }

    public static void challenge(ServerPlayerEntity challenger, ServerPlayerEntity target) {
        if (challenger == target) {
            challenger.sendMessage(error("You can't duel yourself."));
            return;
        }
        if (isInDuel(challenger.getUuid())) {
            challenger.sendMessage(error("You're already in a duel."));
            return;
        }
        if (isInDuel(target.getUuid())) {
            challenger.sendMessage(error(target.getName().getString() + " is already in a duel."));
            return;
        }

        int expiry = challenger.getEntityWorld().getServer().getTicks() + REQUEST_EXPIRY_TICKS;
        pendingRequests.put(target.getUuid(),
                new PendingRequest(challenger.getUuid(), challenger.getName().getString(), expiry));

        challenger.sendMessage(info("Duel request sent to " + target.getName().getString()
                + ". It expires in 30s."));
        target.sendMessage(info(challenger.getName().getString()
                + " challenged you to a duel! Type /duel accept or /duel deny."));
    }

    public static void accept(ServerPlayerEntity target) {
        PendingRequest request = pendingRequests.remove(target.getUuid());
        if (request == null) {
            target.sendMessage(error("You have no pending duel request."));
            return;
        }

        MinecraftServer server = target.getEntityWorld().getServer();
        ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(request.challenger());
        if (challenger == null) {
            target.sendMessage(error(request.challengerName() + " is no longer online."));
            return;
        }
        if (isInDuel(challenger.getUuid()) || isInDuel(target.getUuid())) {
            target.sendMessage(error("One of you is already in a duel."));
            return;
        }

        activeDuels.put(challenger.getUuid(), target.getUuid());
        activeDuels.put(target.getUuid(), challenger.getUuid());

        Text start = info("Duel started: " + challenger.getName().getString()
                + " vs " + target.getName().getString() + ". Fight!");
        challenger.sendMessage(start);
        target.sendMessage(start);
    }

    public static void deny(ServerPlayerEntity target) {
        PendingRequest request = pendingRequests.remove(target.getUuid());
        if (request == null) {
            target.sendMessage(error("You have no pending duel request."));
            return;
        }
        target.sendMessage(info("Duel request denied."));

        ServerPlayerEntity challenger = target.getEntityWorld().getServer()
                .getPlayerManager().getPlayer(request.challenger());
        if (challenger != null) {
            challenger.sendMessage(info(target.getName().getString() + " denied your duel request."));
        }
    }

    public static boolean isInDuel(UUID player) {
        return activeDuels.containsKey(player);
    }

    public static boolean areDueling(UUID a, UUID b) {
        return b.equals(activeDuels.get(a));
    }

    private static void onPlayerDeath(ServerPlayerEntity loser) {
        UUID opponentId = activeDuels.get(loser.getUuid());
        if (opponentId == null) {
            return;
        }
        endDuel(loser.getUuid(), opponentId);

        ServerPlayerEntity winner = loser.getEntityWorld().getServer().getPlayerManager().getPlayer(opponentId);
        if (winner != null) {
            int reward = rewardFor(winner, loser);
            ChampionLevelManager.addXP(winner, reward);
            winner.sendMessage(info("You won the duel against " + loser.getName().getString()
                    + "! +" + reward + " XP"));
        }
        loser.sendMessage(error("You lost the duel."));
    }

    private static void onPlayerLeave(ServerPlayerEntity player, MinecraftServer server) {
        UUID id = player.getUuid();
        pendingRequests.remove(id);
        // drop any request that was aimed at this player from someone else
        pendingRequests.values().removeIf(req -> req.challenger().equals(id));

        UUID opponentId = activeDuels.get(id);
        if (opponentId != null) {
            endDuel(id, opponentId);
            ServerPlayerEntity opponent = server.getPlayerManager().getPlayer(opponentId);
            if (opponent != null) {
                opponent.sendMessage(info(player.getName().getString()
                        + " left the duel. You win by forfeit!"));
            }
        }
    }

    // base + (loserLevel - winnerLevel) * step, floored so it's always worth something.
    private static int rewardFor(ServerPlayerEntity winner, ServerPlayerEntity loser) {
        int winnerLevel = winner.getAttachedOrCreate(PlayerChampionData.ATTACHMENT).champLevel();
        int loserLevel = loser.getAttachedOrCreate(PlayerChampionData.ATTACHMENT).champLevel();
        int reward = BASE_DUEL_XP + (loserLevel - winnerLevel) * XP_PER_LEVEL_GAP;
        return Math.max(MIN_DUEL_XP, reward);
    }

    private static void endDuel(UUID a, UUID b) {
        activeDuels.remove(a);
        activeDuels.remove(b);
    }

    private static void expireRequests(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) {
            return; // check once a second; the exact tick doesn't matter
        }
        int now = server.getTicks();
        Iterator<Map.Entry<UUID, PendingRequest>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingRequest> entry = it.next();
            PendingRequest request = entry.getValue();
            if (now < request.expiryTick()) {
                continue;
            }
            it.remove();

            ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(request.challenger());
            if (challenger != null) {
                challenger.sendMessage(info("Your duel request expired."));
            }
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(entry.getKey());
            if (target != null) {
                target.sendMessage(info("A duel request to you expired."));
            }
        }
    }

    private static Text info(String message) {
        return Text.literal("[Duel] " + message).formatted(Formatting.GOLD);
    }

    private static Text error(String message) {
        return Text.literal("[Duel] " + message).formatted(Formatting.RED);
    }
}
