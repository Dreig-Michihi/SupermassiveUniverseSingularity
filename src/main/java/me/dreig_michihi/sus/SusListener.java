package me.dreig_michihi.sus;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class SusListener implements Listener {
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (!bPlayer.canBend(CoreAbility.getAbility(SupermassiveUniverseSingularity.class)))
			return;
		if (!player.isSneaking()) {
			//player.sendMessage("construct");
			new SupermassiveUniverseSingularity(player);
		}
	}
}
