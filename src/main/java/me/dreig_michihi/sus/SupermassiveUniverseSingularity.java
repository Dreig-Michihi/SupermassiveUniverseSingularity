package me.dreig_michihi.sus;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class SupermassiveUniverseSingularity extends AvatarAbility implements AddonAbility {

	private @Attribute("Gravity") double maxGravity;
	private @Attribute(Attribute.RADIUS) double maxRadius;
	private @Attribute(Attribute.CHARGE_DURATION) long chargeDuration;
	private @Attribute(Attribute.DURATION) long duration;
	private @Attribute(Attribute.DAMAGE) double damage;
	private @Attribute(Attribute.FIRE_TICK) int fireTicks;
	private @Attribute(Attribute.COOLDOWN) long cooldown;

	private Location location;

	private void setFields() {
		String path = "ExtraAbilities.Dreig_Michihi.Avatar.SupermassiveUniverseSingularity.";
		maxGravity = getConfig().getDouble(path + "MaxGravity");
		maxRadius = getConfig().getDouble(path + "MaxRadius");
		chargeDuration = getConfig().getLong(path + "ChargeDuration");
		duration = getConfig().getLong(path + "Duration");
		damage = getConfig().getDouble(path + "Damage");
		fireTicks = getConfig().getInt(path + "FireTicks");
		cooldown = getConfig().getLong(path + "Cooldown");

		location = player.getEyeLocation().add(player.getLocation().getDirection()); // initial location
	}

	public SupermassiveUniverseSingularity(Player player) {
		super(player);
		if (bPlayer == null) {
			//player.sendMessage("abil disabled");
			return; // ability disabled
		}
		if (!bPlayer.canBend(this)) {
			//player.sendMessage("!player can bend");
			return;
		}
		setFields();
		start();
	}

	private double gravity = 0;
	private double radius = 0;
	private double factor = 0;

	private void updateValuesGrowing() {
		if (factor > 0) {
			gravity = maxGravity * factor;
			radius = maxRadius * Math.sqrt(factor);
			//location = player.getEyeLocation().add(player.getLocation().getDirection()).add(player.getLocation().getDirection().multiply(radius));
		}
	}

	private Vector getRandom() {
		double pitch = ThreadLocalRandom.current().nextDouble(-90, 90);
		double yaw = ThreadLocalRandom.current().nextDouble(-180, 180);
		return new Vector(-Math.cos(pitch) * Math.sin(yaw), -Math.sin(pitch), Math.cos(pitch) * Math.cos(yaw));
	}

	private void renderCharging() {
		double charge = (double) (System.currentTimeMillis() - getStartTime()) / chargeDuration;
		ParticleEffect.SQUID_INK.display(getLocation(), 10, 0.3 * charge, 0.3 * charge, 0.3 * charge);
		getLocation().getWorld().spawnParticle(Particle.ELECTRIC_SPARK, getLocation(), 3, 3 * charge, 3 * charge, 3 * charge);

		//(new ColoredParticle(Color.fromRGB(0, 0, 0), 50)).display(getLocation(), 5, 0.5, 0.5, 0.5);
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 3; j++) {
				Vector random = getRandom();
				double randomLength = ThreadLocalRandom.current().nextDouble(maxRadius);
				RayTraceResult result = player.getWorld().rayTraceBlocks(getLocation(), random, i < 2 ? randomLength : maxRadius, FluidCollisionMode.ALWAYS, true);
				if (result == null)
					random.multiply(randomLength);
				else {
					if (result.getHitBlock() != null)
						random.multiply(getLocation().distance(result.getHitBlock().getLocation().add(.5, .5, .5)));
				}
				Location particleLocation = getLocation().add(random);
				switch (i) {
					case 0: { // fire particle
						(bPlayer.canUseSubElement(Element.SubElement.BLUE_FIRE) ? ParticleEffect.SOUL_FIRE_FLAME : ParticleEffect.FLAME)
								.display(particleLocation, 0, -random.getX(), -random.getY(), -random.getZ(), 0.05);
						FireAbility.playFirebendingSound(particleLocation);
						break;
					}
					case 1: { // air particle
						ParticleEffect.CLOUD.display(particleLocation, 0, -random.getX(), -random.getY(), -random.getZ(), 0.05);
						AirAbility.playAirbendingSound(particleLocation);
						break;
					}
					case 2: { // water particle
						if (result != null && isWater(result.getHitBlock()))
							ParticleEffect.WATER_SPLASH.display(result.getHitBlock().getLocation().add(.5, .5, .5), 15, 0.7, 0.7, 0.7, 0.15);
						ParticleEffect.WATER_WAKE.display(particleLocation, 0, -random.getX(), -random.getY(), -random.getZ(), 0.05);
						WaterAbility.playWaterbendingSound(particleLocation);
						break;
					}
					case 3: { // earth particle
						Block ground = result == null ? GeneralMethods.getTopBlock(particleLocation, 0, (int) (maxRadius * 2)) : result.getHitBlock();
						if (ground != null) {
							if (!EarthAbility.isEarthbendable(player, ground))
								break;
							BlockData bd = ground.getBlockData();
							Vector vec = GeneralMethods.getDirection(ground.getLocation().add(.5, 1, .5), getLocation());
							Location pLoc = ground.getLocation().add(ThreadLocalRandom.current().nextDouble(), 1, ThreadLocalRandom.current().nextDouble());
							ParticleEffect.BLOCK_CRACK.display(pLoc, 15, .5, .5, .5, 0.15, bd);
							ParticleEffect.BLOCK_CRACK.display(pLoc, 0, vec.getX(), vec.getY(), vec.getZ(), 0.05, bd);
							EarthAbility.playEarthbendingSound(pLoc);
						}
						break;
					}
				}
			}
		}
	}

	private boolean charged = false;

	private void gravity() {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(getLocation(), radius)) {
			if (entity == player)
				continue;
			GeneralMethods.setVelocity(this, entity,
					entity.getVelocity().add(
							GeneralMethods.getDirection(entity.getLocation(), getLocation()).normalize().multiply(factor * (1))));
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this) || (factor >= 1)) {
			if (factor == 1) {
				new BukkitRunnable() {
					@Override
					public void run() {
						ParticleEffect.EXPLOSION_HUGE.display(getLocation(), 1, 0, 0, 0);
						getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 5, 0f);
						getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 5, 2f);
						getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 5, 5f);
						getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 5, 1f);
						getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 5, 0f);
						for (Entity entity : GeneralMethods.getEntitiesAroundPoint(getLocation(), 2)) {
							if (entity == player)
								continue;
							if (location != null && entity instanceof LivingEntity) {
								DamageHandler.damageEntity(entity, damage, SupermassiveUniverseSingularity.this);
							}
						}
						remove();
					}
				}.runTaskLater(ProjectKorra.plugin, 50);
				factor++;
			}
			gravity();
			return;
		}
		factor = Math.max(0, Math.min(1, (double) (System.currentTimeMillis() - (getStartTime() + chargeDuration)) / (duration)));
		updateValuesGrowing();
		if (!charged) {
			if (!bPlayer.getBoundAbility().equals(CoreAbility.getAbility(getClass())) || !player.isSneaking()) {
				remove();
				return;
			}
			location.add(player.getLocation().getDirection().multiply(.7));
			if (factor == 0)
				renderCharging();
			else {

				charged = true;
				bPlayer.addCooldown(this);
				ParticleEffect.EXPLOSION_HUGE.display(getLocation(), 1, 0, 0, 0);
				getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 5, 0f);
				getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 5, 2f);
				getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 5, 5f);
				getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 5, 1f);
				getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 5, 0f);
				location = location.getBlock().getLocation().add(.5, .5, .5);
				//private boolean supernova = false;
				TempBlock hole = new TempBlock(location.getBlock(), Material.COAL_BLOCK.createBlockData(), duration + 200);
				hole.setRevertTask(() -> new TempBlock(location.getBlock(), Material.BARRIER.createBlockData(), 2500));
			}
		} else if (factor != 1) {
			ParticleEffect.FLASH.display(getLocation(), 15);
			for (int i = 0; i < factor * 100; i++) {
				Vector r = getRandom().multiply(radius);
				ParticleEffect.END_ROD.display(getLocation().add(r), 0, -r.getX(), -r.getY(), -r.getZ(), 0.09);
				//ParticleEffect.END_ROD.display(getLocation(), 0, r.getX(), r.getY(), r.getZ(), 0.08);
			}

			gravity();

			if (ThreadLocalRandom.current().nextBoolean())
				getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.PLAYERS, 5, 0f);
		}
	}

	@Override
	public String getMovePreview(Player player) {
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		String displayedMessage = this.getMovePreviewWithoutCooldownTimer(player, false);
		if (bPlayer.isOnCooldown(this)) {
			long cooldown = bPlayer.getCooldown(this.getName()) - System.currentTimeMillis();
			displayedMessage = displayedMessage + this.getElement().getColor() + " - " + TimeUtil.formatTime(cooldown);
		}

		return displayedMessage;
	}

	@Override
	public String getMovePreviewWithoutCooldownTimer(Player player, boolean forceCooldown) {
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		String displayedMessage = "";
		if (!forceCooldown && !bPlayer.isOnCooldown(this)) {
			boolean isActiveStance = bPlayer.getStance() != null && bPlayer.getStance().getName().equals(this.getName());
			boolean isActiveAvatarState = bPlayer.isAvatarState() && this.getName().equals("AvatarState");
			boolean isActiveIllumination = bPlayer.isIlluminating() && this.getName().equals("Illumination");
			boolean isActiveTremorSense = bPlayer.isTremorSensing() && this.getName().equals("Tremorsense");
			if (!isActiveStance && !isActiveAvatarState && !isActiveIllumination && !isActiveTremorSense) {
				displayedMessage = this.getElement().getColor() + this.getName();
			} else {
				displayedMessage = this.getElement().getColor() + "" + ChatColor.UNDERLINE + this.getName();
			}
		} else {
			displayedMessage = this.getElement().getColor() + "" + ChatColor.STRIKETHROUGH + "SUS";
		}

		return displayedMessage;
	}

	@Override
	public void remove() {
		super.remove();
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public String getName() {
		return "SupermassiveUniverseSingularity";
	}

	@Override
	public Location getLocation() {
		return location == null ? null : location.clone();
	}

	private static Listener listener;

	@Override
	public void load() {
		String path = "ExtraAbilities.Dreig_Michihi.Avatar.SupermassiveUniverseSingularity.";
		getConfig().addDefault(path + "MaxGravity", 2);
		getConfig().addDefault(path + "MaxRadius", 30);
		getConfig().addDefault(path + "ChargeDuration", 1500);
		getConfig().addDefault(path + "Duration", 5000);
		getConfig().addDefault(path + "Damage", 3);
		getConfig().addDefault(path + "FireTicks", 20);
		getConfig().addDefault(path + "Cooldown", 5000);
		getLanguageConfig().addDefault("Abilities." + getElement().getName() + "." + this.getName() + ".Description", "Compresses fire, air, water, and earth into a small sphere, creating a black hole");
		getLanguageConfig().addDefault("Abilities." + getElement().getName() + "." + this.getName() + ".Instructions", "Hold sneak until the black hole will be created.");
		ConfigManager.defaultConfig.save();

		listener = new SusListener();
		Bukkit.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);
		ProjectKorra.log.info(getName() + "[" + getVersion() + "] " + " by " + getAuthor() + " has been loaded!");
	}

	@Override
	public void stop() {
		HandlerList.unregisterAll(listener);
		ProjectKorra.log.info(getName() + "[" + getVersion() + "] " + " by " + getAuthor() + " has been stopped!");
		listener = null;
	}

	@Override
	public String getAuthor() {
		return "Dreig_Michihi";
	}

	@Override
	public String getVersion() {
		return "v(-1)";
	}
}
