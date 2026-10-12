package com.survivaltweaks;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Per-world state of the mod, saved in a plain text file that does NOT depend on
 * Minecraft's or Fabric's save formats:
 *
 *     <world folder>/data/survivaltweaks/world_state.properties
 *
 * Contents look like:
 *
 *     dragon_defeated=true
 *
 * Any future version of this mod (26.4, 27.x, ...) only has to read that one line
 * to know the dragon was already killed in this world. You can also open the file
 * in Notepad and change it by hand (do that while the world is closed).
 *
 * As a second safety net the flag is ALSO stored as a Fabric data attachment (this
 * is how version 1.0.0 stored it). On world load the two are combined, so worlds
 * from 1.0.0 are migrated into the file automatically.
 *
 * DO NOT rename FILE_DIR, FILE_NAME, KEY_DRAGON_DEFEATED or the attachment id -
 * existing worlds would lose their progress.
 */
public final class WorldState {
    private WorldState() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SurvivalTweaks.MOD_ID);

    private static final String FILE_DIR = SurvivalTweaks.MOD_ID;
    private static final String FILE_NAME = "world_state.properties";
    private static final String KEY_DRAGON_DEFEATED = "dragon_defeated";

    /** Backup copy inside the normal world save (the 1.0.0 storage). */
    public static final AttachmentType<Boolean> DRAGON_DEFEATED_ATTACHMENT =
            AttachmentRegistry.createPersistent(SurvivalTweaks.id("dragon_defeated"), Codec.BOOL);

    /** Cached value of the file for the currently open world. */
    private static volatile boolean dragonDefeatedInFile = false;

    public static void register() {
        // Read the file as early as possible, before mobs start spawning.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> dragonDefeatedInFile = readFile(server));

        // Once everything is loaded: migrate 1.0.0 worlds (attachment only -> also file).
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!dragonDefeatedInFile && attachmentSaysDefeated(server)) {
                dragonDefeatedInFile = true;
                writeFile(server);
            }
        });

        // Singleplayer: the "server" stops when you leave a world - forget its state.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> dragonDefeatedInFile = false);
    }

    public static boolean isDragonDefeated(MinecraftServer server) {
        return dragonDefeatedInFile || attachmentSaysDefeated(server);
    }

    public static void setDragonDefeated(MinecraftServer server) {
        dragonDefeatedInFile = true;
        server.globalAttachments().setAttached(DRAGON_DEFEATED_ATTACHMENT, true);
        writeFile(server);
    }

    private static boolean attachmentSaysDefeated(MinecraftServer server) {
        return Boolean.TRUE.equals(server.globalAttachments().getAttached(DRAGON_DEFEATED_ATTACHMENT));
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.DATA).resolve(FILE_DIR).resolve(FILE_NAME);
    }

    private static boolean readFile(MinecraftServer server) {
        Path path = file(server);
        if (!Files.exists(path)) {
            return false;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            LOGGER.error("[Survival Tweaks] Could not read {}", path, e);
            return false;
        }
        return Boolean.parseBoolean(props.getProperty(KEY_DRAGON_DEFEATED, "false").trim());
    }

    private static void writeFile(MinecraftServer server) {
        Path path = file(server);
        Properties props = new Properties();
        props.setProperty(KEY_DRAGON_DEFEATED, Boolean.toString(dragonDefeatedInFile));
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                props.store(writer, "Survival Tweaks world state. Safe to edit while the world is closed.");
            }
        } catch (IOException e) {
            LOGGER.error("[Survival Tweaks] Could not write {}", path, e);
        }
    }
}
