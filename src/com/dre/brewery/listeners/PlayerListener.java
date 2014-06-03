package com.dre.brewery.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BIngredients;
import com.dre.brewery.Brew;
import com.dre.brewery.Barrel;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Words;
import com.dre.brewery.Wakeup;
import com.dre.brewery.P;


public class PlayerListener implements Listener {
	public static boolean openEverywhere;

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();

		if (clickedBlock != null) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Player player = event.getPlayer();
				if (!player.isSneaking()) {
					Material type = clickedBlock.getType();
					if (type == Material.CAULDRON) {
						Block down = clickedBlock.getRelative(BlockFace.DOWN);
						if (down.getType() == Material.FIRE || down.getType() == Material.STATIONARY_LAVA || down.getType() == Material.LAVA) {
							Material materialInHand = event.getMaterial();
							ItemStack item = event.getItem();

							
							if (materialInHand == Material.WATCH) {
								BCauldron.printTime(player, clickedBlock);

								// fill a glass bottle with potion
							} else if (materialInHand == Material.GLASS_BOTTLE) {
								if (player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
									if (BCauldron.fill(player, clickedBlock)) {
										event.setCancelled(true);
										if (player.hasPermission("brewery.cauldron.fill")) {
											if (item.getAmount() > 1) {
												item.setAmount(item.getAmount() - 1);
											} else {
												player.setItemInHand(new ItemStack(Material.AIR));
											}
										}
									}
								} else {
									event.setCancelled(true);
								}

								// reset cauldron when refilling to prevent
								// unlimited source of potions
							} else if (materialInHand == Material.WATER_BUCKET) {
								if (BCauldron.getFillLevel(clickedBlock) != 0) {
									if (BCauldron.getFillLevel(clickedBlock) < 2) {
										// will only remove when existing
										BCauldron.remove(clickedBlock);
									}
								}

								// add ingredient to cauldron that meet the previous
								// contitions
							} else if (BIngredients.possibleIngredients.contains(materialInHand)) {
								if (player.hasPermission("brewery.cauldron.insert")) {
									if (BCauldron.ingredientAdd(clickedBlock, materialInHand)) {
										boolean isBucket = item.getType().equals(Material.WATER_BUCKET)
												|| item.getType().equals(Material.LAVA_BUCKET)
												|| item.getType().equals(Material.MILK_BUCKET);
										if (item.getAmount() > 1) {
											item.setAmount(item.getAmount() - 1);

											if (isBucket) {
												BCauldron.giveItem(player, new ItemStack(Material.BUCKET));
											}
										} else {
											if (isBucket) {
												player.setItemInHand(new ItemStack(Material.BUCKET));
											} else {
												player.setItemInHand(new ItemStack(Material.AIR));
											}
										}
									}
								} else {
									P.p.msg(player, P.p.languageReader.get("Perms_NoCauldronInsert"));
								}
								event.setCancelled(true);
							} else {
								event.setCancelled(true);
							}
							return;
						}
					}

					// Access a Barrel
					Barrel barrel = null;
					if (type == Material.WOOD) {
						if (openEverywhere) {
							barrel = Barrel.get(clickedBlock);
						}
					} else if (Barrel.isStairs(type)) {
						for (Barrel barrel2 : Barrel.barrels) {
							if (barrel2.hasStairsBlock(clickedBlock)) {
								if (openEverywhere || !barrel2.isLarge()) {
									barrel = barrel2;
								}
								break;
							}
						}
					} else if (type == Material.FENCE || type == Material.NETHER_FENCE || type == Material.SIGN_POST || type == Material.WALL_SIGN) {
						barrel = Barrel.getBySpigot(clickedBlock);
					}

					if (barrel != null) {
						event.setCancelled(true);

						if (!barrel.hasPermsOpen(player, event)) {
							return;
						}

						barrel.open(player);
					}
				}
			}
		}

		if (event.getAction() == Action.LEFT_CLICK_AIR) {
			if (!event.hasItem()) {
				if (Wakeup.checkPlayer != null) {
					if (event.getPlayer() == Wakeup.checkPlayer) {
						Wakeup.tpNext();
					}
				}
			}
		}

	}

	// player drinks a custom potion
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item != null) {
			if (item.getType() == Material.POTION) {
				if (item.hasItemMeta()) {
					if (BPlayer.drink(Brew.getUID(item), player)) {
						if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
							Brew.remove(item);
						}
					}
				}
			} else if (BPlayer.drainItems.containsKey(item.getType())) {
				BPlayer bplayer = BPlayer.get(player.getName());
				if (bplayer != null) {
					bplayer.drainByItem(player.getName(), item.getType());
				}
			}
		}
	}

	// Player has died! Decrease Drunkeness by 20
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		String playerName = event.getPlayer().getName();
		BPlayer bPlayer = BPlayer.get(playerName);
		if (bPlayer != null) {
			if (bPlayer.getDrunkeness() > 20) {
				bPlayer.setData(bPlayer.getDrunkeness() - 20, 0);
			} else {
				BPlayer.players.remove(playerName);
			}
		}
	}

	// player walks while drunk, push him around!
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (BPlayer.players.containsKey(event.getPlayer().getName())) {
			BPlayer.playerMove(event);
		}
	}

	// player talks while drunk, but he cant speak very well
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (BPlayer.players.containsKey(event.getPlayer().getName())) {
			Words.playerChat(event);
		}
	}
	
	// player commands while drunk, distort chat commands
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
		if (BPlayer.players.containsKey(event.getPlayer().getName())) {
			Words.playerCommand(event);
		}
	}

	// player joins while passed out
	@EventHandler()
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
			final Player player = event.getPlayer();
			BPlayer bplayer = BPlayer.get(player.getName());
			if (bplayer != null) {
				if (player.hasPermission("brewery.bypass.logindeny")) {
					if (bplayer.getDrunkeness() > 100) {
						bplayer.setData(100, 0);
					}
					bplayer.join(player);
					return;
				}
				switch (bplayer.canJoin()) {
					case 0:
						bplayer.join(player);
						return;
					case 2:
						event.disallow(PlayerLoginEvent.Result.KICK_OTHER, P.p.languageReader.get("Player_LoginDeny"));
						return;
					case 3:
						event.disallow(PlayerLoginEvent.Result.KICK_OTHER, P.p.languageReader.get("Player_LoginDenyLong"));
				}
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer().getName());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer().getName());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
	}
}