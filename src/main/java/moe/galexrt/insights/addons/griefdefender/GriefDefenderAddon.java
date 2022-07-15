package moe.galexrt.insights.addons.griefdefender;

import dev.frankheijden.insights.api.InsightsPlugin;
import dev.frankheijden.insights.api.addons.InsightsAddon;
import dev.frankheijden.insights.api.addons.Region;
import dev.frankheijden.insights.api.addons.SimpleCuboidRegion;
import dev.frankheijden.insights.api.objects.math.Vector3;

import com.google.common.eventbus.Subscribe;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.event.ChangeClaimEvent;
import com.griefdefender.api.event.RemoveClaimEvent;
import com.griefdefender.lib.flowpowered.math.vector.Vector3i;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Listener;
import java.util.Optional;

public class GriefDefenderAddon implements InsightsAddon, Listener {

    public String getId(Claim claim) {
        return getPluginName() + "@" + claim.getUniqueId();
    }

    public GriefDefenderAddon() {
        // Register our GriefDefender events listeners just when the server started
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Insights"), () -> {
            GriefDefender.getEventManager().getBus().subscribe(ChangeClaimEvent.class, this::onChangeClaimEvent);
            GriefDefender.getEventManager().getBus().subscribe(RemoveClaimEvent.class, this::onRemoveClaimEvent);
        });
    }

    public Optional<Region> adapt(Claim claim) {
        if (claim == null)
            return Optional.empty();
        if (claim.isWilderness())
            return Optional.empty();

        Vector3i minVector = claim.getLesserBoundaryCorner();
        Vector3i maxVector = claim.getGreaterBoundaryCorner();

        World world = Bukkit.getWorld(claim.getWorldUniqueId());
        Location min = new Location(world, minVector.getX(), minVector.getY(), minVector.getZ());
        Location max = new Location(world, maxVector.getX(), maxVector.getY(), maxVector.getZ());

        return Optional.of(new SimpleCuboidRegion(
                min.getWorld(),
                new Vector3(min.getBlockX(), max.getWorld().getMinHeight(), min.getBlockZ()),
                new Vector3(max.getBlockX(), max.getWorld().getMaxHeight() - 1, max.getBlockZ()),
                getPluginName(),
                getId(claim)));
    }

    @Override
    public String getPluginName() {
        return "GriefDefender";
    }

    @Override
    public String getAreaName() {
        return "claim";
    }

    @Override
    public String getVersion() {
        return "{version}";
    }

    @Override
    public Optional<Region> getRegion(Location location) {
        return adapt(GriefDefender.getCore().getClaimManager(location.getWorld().getUID()).getClaimAt(location.getBlockX(),
                location.getBlockY(), location.getBlockZ()));
    }

    @Subscribe
    public void onChangeClaimEvent(ChangeClaimEvent event) {
        if (event.cancelled()) return;

        deleteClaimCache(event.getClaim());
    }

    @Subscribe
    public void onRemoveClaimEvent(RemoveClaimEvent event) {
        if (event.cancelled()) return;

        deleteClaimCache(event.getClaim());
    }

    private void deleteClaimCache(Claim claim) {
        InsightsPlugin.getInstance().getAddonStorage().remove(getId(claim));
    }
}
