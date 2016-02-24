/*
 * This file is part of QuickStart, licensed under the MIT License (MIT). See the LICENCE.txt file
 * at the root of this project for more details.
 */
package uk.co.drnaylor.minecraft.quickstart.internal.services;

import com.google.common.collect.Maps;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Identifiable;
import uk.co.drnaylor.minecraft.quickstart.QuickStart;
import uk.co.drnaylor.minecraft.quickstart.api.data.QuickStartUser;
import uk.co.drnaylor.minecraft.quickstart.api.exceptions.NoSuchPlayerException;
import uk.co.drnaylor.minecraft.quickstart.api.service.QuickStartUserService;
import uk.co.drnaylor.minecraft.quickstart.internal.interfaces.InternalQuickStartUser;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserConfigLoader implements QuickStartUserService {

    private final QuickStart plugin;
    private final Map<UUID, UserService> loadedUsers = Maps.newHashMap();
    private final Map<UUID, SoftReference<UserService>> softLoadedUsers = Maps.newHashMap();

    public UserConfigLoader(QuickStart plugin) {
        this.plugin = plugin;
    }

    public List<QuickStartUser> getOnlineUsers() {
        return Sponge.getServer().getOnlinePlayers().stream().map(x -> {
            try {
                return getUser(x);
            } catch (IOException | ObjectMappingException e) {
                e.printStackTrace();
            }

            return null;
        }).filter(x -> x != null).collect(Collectors.toList());
    }

    @Override
    public InternalQuickStartUser getUser(UUID playerUUID) throws NoSuchPlayerException, IOException, ObjectMappingException {
        return getUser(Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(playerUUID).orElseThrow(NoSuchPlayerException::new));
    }

    @Override
    public InternalQuickStartUser getUser(User user) throws IOException, ObjectMappingException {
        if (loadedUsers.containsKey(user.getUniqueId())) {
            return loadedUsers.get(user.getUniqueId());
        }

        // If we have a soft reference, move them into the strong reference queue before returning them.
        // The act of moving back into the soft loaded queue is a save. Soft loaded references are really
        // just acting as a cache.
        clearNullSoftReferences();
        if (softLoadedUsers.containsKey(user.getUniqueId())) {
            UserService uc = softLoadedUsers.get(user.getUniqueId()).get();
            softLoadedUsers.remove(user.getUniqueId());
            loadedUsers.put(user.getUniqueId(), uc);
            return uc;
        }

        // Load the file in.
        UserService uc = new UserService(plugin, getUserPath(user.getUniqueId()), user);
        loadedUsers.put(user.getUniqueId(), uc);
        return uc;
    }

    public void saveAll() {
        loadedUsers.values().forEach(c -> {
            try {
                c.save();
            } catch (IOException | ObjectMappingException e) {
                plugin.getLogger().error("Could not save data for " + c.getUniqueID().toString());
                e.printStackTrace();
            }
        });
    }

    public void purgeNotOnline() {
        Set<UUID> onlineUUIDs = Sponge.getServer().getOnlinePlayers().stream().map(Identifiable::getUniqueId).collect(Collectors.toSet());

        // Collector to list should prevent CMEs.
        loadedUsers.keySet().stream().filter(x -> !onlineUUIDs.contains(x)).collect(Collectors.toList()).forEach(x -> {
            try {
                UserService uc = loadedUsers.get(x);
                uc.save();
                loadedUsers.remove(x);
                softLoadedUsers.put(x, new SoftReference<>(uc));
            } catch (IOException | ObjectMappingException e) {
                plugin.getLogger().error("Could not save data for " + x.toString());
                e.printStackTrace();
            }
        });
    }

    public void forceUnloadPlayerWithoutSaving(UUID uuid) {
        purgeNotOnline();
        if (loadedUsers.containsKey(uuid)) {
            // Really is no need to save at this point.
            loadedUsers.remove(uuid);
        }

        if (softLoadedUsers.containsKey(uuid)) {
            softLoadedUsers.remove(uuid);
        }
    }

    private void clearNullSoftReferences() {
        softLoadedUsers.entrySet().stream().filter(k -> k.getValue().get() == null).map(Map.Entry::getKey).forEach(softLoadedUsers::remove);
    }

    public Path getUserPath(UUID uuid) throws IOException {
        String u = uuid.toString();
        String f = u.substring(0, 2);
        Path file = plugin.getDataPath().resolve(String.format("userdata%1$s%2$s%1$s%3$s.json", File.separator, f, u));

        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
        }

        // Configurate will create it for us.
        return file;
    }
}
