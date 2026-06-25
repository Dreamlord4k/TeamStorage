package ru.syntaxteam.teamstorage.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.syntaxteam.teamstorage.service.CityChatService;
import ru.syntaxteam.teamstorage.service.TabNameService;

public final class CityChatListener implements Listener {
    private final JavaPlugin plugin;
    private final CityChatService cityChatService;
    private final TabNameService tabNameService;

    public CityChatListener(JavaPlugin plugin, CityChatService cityChatService, TabNameService tabNameService) {
        this.plugin = plugin;
        this.cityChatService = cityChatService;
        this.tabNameService = tabNameService;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        CityChatService.ChatDecision decision = cityChatService.prepare(player, plainMessage);

        if (!decision.cancelOriginal()) {
            if (!decision.message().equals(plainMessage)) {
                event.message(Component.text(decision.message()));
            }
            return;
        }

        event.setCancelled(true);
        if (decision.cityMessage()) {
            Bukkit.getScheduler().runTask(plugin, () -> cityChatService.sendCityMessage(player, decision.city(), decision.message()));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> tabNameService.refresh(event.getPlayer()), 20L);
    }
}
