package com.leclowndu93150.bettercombatextension.mixin.client;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import com.leclowndu93150.bettercombatextension.bettercombat.DuckWeaponAttributesAttackMixin;
import com.leclowndu93150.bettercombatextension.client.DuckMinecraftClientMixin;
import com.leclowndu93150.bettercombatextension.config.ServerConfig;
import com.leclowndu93150.bettercombatextension.entity.DuckLivingEntityMixin;
import com.leclowndu93150.bettercombatextension.packet.AttackStaminaCostPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.autoconfig.AutoConfig;
import net.bettercombat.BetterCombat;
import net.bettercombat.PlatformClient;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.bettercombat.client.BetterCombatClient;
import net.bettercombat.client.BetterCombatKeybindings;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.client.collision.TargetFinder;
import net.bettercombat.config.ClientConfigWrapper;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerAttackProperties;
import net.bettercombat.logic.WeaponRegistry;
import net.bettercombat.mixin.client.MinecraftClientAccessor;
import net.bettercombat.network.Packets;
import net.bettercombat.utils.PatternMatching;
import com.leclowndu93150.bettercombatextension.network.ExtensionNetworkHandler;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.bettercombat.forge.network.NetworkHandler;
import net.bettercombat.forge.network.PacketWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Mixin({Minecraft.class})
public abstract class MinecraftClient_BetterCombatReplacementMixin implements MinecraftClient_BetterCombat, DuckMinecraftClientMixin {
	@Shadow
	public ClientLevel level;
	@Shadow
	public @Nullable LocalPlayer player;
	@Shadow
	private int rightClickDelay;
	@Shadow
	public int missTime;
	@Unique
	private boolean isHoldingAttackInput = false;
	@Unique
	private boolean isHarvesting = false;
	@Unique
	private String textToRender = null;
	@Unique
	private int textFade = 0;
	@Unique
	private ItemStack upswingStack;
	@Unique
	private ItemStack lastAttacedWithItemStack;
	@Unique
	private int upswingTicks = 0;
	@Unique
	private int lastAttacked = 1000;
	@Unique
	private float lastSwingDuration = 0.0F;
	@Unique
	private int comboReset = 0;
	@Unique
	private List<Entity> targetsInReach = null;

	public MinecraftClient_BetterCombatReplacementMixin() {
	}

	@Unique
	private Minecraft thisClient() {
		return (Minecraft) (Object) this;
	}

	@Inject(
			method = {"<init>"},
			at = {@At("TAIL")}
	)
	private void postInit(GameConfig args, CallbackInfo ci) {
		this.setupTextRenderer();
	}

	@Inject(
			method = {"clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V"},
			at = {@At("TAIL")}
	)
	private void disconnect_TAIL(Screen screen, CallbackInfo ci) {
		BetterCombatClient.ENABLED = false;
	}

	@Unique
	private void setupTextRenderer() {
		MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
			if (this.textToRender != null && !this.textToRender.isEmpty()) {
				Minecraft client = Minecraft.getInstance();
				Font textRenderer = client.gui.getFont();
				int scaledWidth = client.getWindow().getGuiScaledWidth();
				int scaledHeight = client.getWindow().getGuiScaledHeight();
				int i = textRenderer.width(this.textToRender);
				int j = (scaledWidth - i) / 2;
				int k = scaledHeight - 59 - 14;
				if (!client.gameMode.canHurtPlayer()) {
					k += 14;
				}

				int l;
				if ((l = (int) ((float) this.textFade * 256.0F / 10.0F)) > 255) {
					l = 255;
				}

				if (l > 0) {
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
					int var10001 = j - 2;
					int var10002 = k - 2;
					int var10003 = j + i + 2;
					Objects.requireNonNull(textRenderer);
					event.getGuiGraphics().fill(var10001, var10002, var10003, k + 9 + 2, client.options.getBackgroundColor(0));
					event.getGuiGraphics().drawString(textRenderer, this.textToRender, j, k, 16777215 + (l << 24));
					RenderSystem.disableBlend();
				}
			}

			if (this.textFade <= 0) {
				this.textToRender = null;
			}

		});
	}

	@Inject(
			method = {"startAttack()Z"},
			at = {@At("HEAD")},
			cancellable = true
	)
	private void pre_doAttack(CallbackInfoReturnable<Boolean> info) {
		if (BetterCombatClient.ENABLED) {
			Minecraft client = this.thisClient();
			ItemStack mainHandStack = client.player.getMainHandItem();
			WeaponAttributes attributes = WeaponRegistry.getAttributes(mainHandStack);
			if (attributes != null && attributes.attacks() != null) {
				if (this.isTargetingMineableBlock() || this.isHarvesting) {
					this.isHarvesting = true;
					return;
				}

				this.startUpswing(attributes);
				info.setReturnValue(false);
				info.cancel();
			}

		}
	}

	@Inject(
			method = {"continueAttack(Z)V"},
			at = {@At("HEAD")},
			cancellable = true
	)
	private void pre_handleBlockBreaking(boolean bl, CallbackInfo ci) {
		if (BetterCombatClient.ENABLED) {
			Minecraft client = this.thisClient();
			ItemStack mainHandStack = client.player.getMainHandItem();
			WeaponAttributes attributes = WeaponRegistry.getAttributes(mainHandStack);
			if (attributes != null && attributes.attacks() != null) {
				boolean isPressed = client.options.keyAttack.isDown();
				if (isPressed && !this.isHoldingAttackInput) {
					if (this.isTargetingMineableBlock() || this.isHarvesting) {
						this.isHarvesting = true;
						return;
					}

					ci.cancel();
				}

				if (BetterCombatClient.config.isHoldToAttackEnabled && !BetterCombatExtension.SERVER_CONFIG.disable_better_combat_hold_to_attack.get() && isPressed) {
					this.isHoldingAttackInput = true;
					this.startUpswing(attributes);
					ci.cancel();
				} else {
					this.isHarvesting = false;
					this.isHoldingAttackInput = false;
				}
			}

		}
	}

	@Inject(
			method = {"startUseItem"},
			at = {@At("HEAD")},
			cancellable = true
	)
	private void pre_doItemUse(CallbackInfo ci) {
		if (BetterCombatClient.ENABLED) {
			AttackHand hand = this.getCurrentHand();
			if (hand != null) {
				double upswingRate = hand.upswingRate();
				if (this.upswingTicks > 0 || (double) this.player.getAttackStrengthScale(0.0F) < 1.0 - upswingRate) {
					ci.cancel();
				}

			}
		}
	}

	@Unique
	private boolean isTargetingMineableBlock() {
		if (!BetterCombatClient.config.isMiningWithWeaponsEnabled) {
			return false;
		} else {
			String regex = BetterCombatClient.config.mineWithWeaponBlacklist;
			if (regex != null && !regex.isEmpty()) {
				ItemStack itemStack = this.player.getMainHandItem();
				String id = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
				if (PatternMatching.matches(id, regex)) {
					return false;
				}
			}

			if (BetterCombatClient.config.isAttackInsteadOfMineWhenEnemiesCloseEnabled && this.hasTargetsInReach()) {
				return false;
			} else {
				Minecraft client = this.thisClient();
				HitResult crosshairTarget = client.hitResult;
				if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.BLOCK) {
					BlockHitResult blockHitResult = (BlockHitResult) crosshairTarget;
					BlockPos pos = blockHitResult.getBlockPos();
					BlockState clicked = this.level.getBlockState(pos);
					if (!this.shouldSwingThruGrass()) {
						return true;
					}

					if (!clicked.getCollisionShape(this.level, pos).isEmpty() || clicked.getDestroySpeed(this.level, pos) != 0.0F) {
						return true;
					}
				}

				return false;
			}
		}
	}

	@Unique
	private boolean shouldSwingThruGrass() {
		if (!BetterCombatClient.config.isSwingThruGrassEnabled) {
			return false;
		} else {
			String regex = BetterCombatClient.config.swingThruGrassBlacklist;
			if (regex != null && !regex.isEmpty()) {
				ItemStack itemStack = this.player.getMainHandItem();
				String id = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
				return !PatternMatching.matches(id, regex);
			} else {
				return true;
			}
		}
	}

	@Unique
	private void startUpswing(WeaponAttributes attributes) {
		if (!this.player.isHandsBusy()) {
			AttackHand hand = this.getCurrentHand();
			if (hand != null) {
				float upswingRate = (float) hand.upswingRate();
				if (this.upswingTicks <= 0 && this.missTime <= 0 && !this.player.isUsingItem() && !((double) this.player.getAttackStrengthScale(0.0F) < 1.0 - (double) upswingRate)) {
					if (BetterCombatExtension.isStaminaAttributesLoaded && ((DuckWeaponAttributesAttackMixin) (Object) hand.attack()).bettercombatextension$getStaminaCostMultiplier() > 0.0F && ((DuckLivingEntityMixin) player).bettercombatextension$getAttackStaminaCost() > 0.0F && BetterCombatExtension.getCurrentStamina(this.player)/* * BetterCombatExtension.serverConfig.global_attack_stamina_cost_multiplier*/ <= 0.0F && !this.player.isCreative()) {
						this.player.displayClientMessage(Component.translatable("hud.message.staminaTooLow"), true);
						return;
					}
					this.player.releaseUsingItem();
					this.lastAttacked = 0;
					this.upswingStack = this.player.getMainHandItem();
					float attackCooldownTicksFloat = PlayerAttackHelper.getAttackCooldownTicksCapped(this.player);
					int attackCooldownTicks = Math.round(attackCooldownTicksFloat);
					this.comboReset = Math.round(attackCooldownTicksFloat * BetterCombat.config.combo_reset_rate);
					this.upswingTicks = Math.max(Math.round(attackCooldownTicksFloat * upswingRate), 1);
					this.lastSwingDuration = attackCooldownTicksFloat;
					this.rightClickDelay = attackCooldownTicks;
					this.setMiningCooldown(attackCooldownTicks);
					String animationName = hand.attack().animation();
					boolean isOffHand = hand.isOffHand();
					AnimatedHand animatedHand = AnimatedHand.from(isOffHand, attributes.isTwoHanded());
					((PlayerAttackAnimatable) this.player).playAttackAnimation(animationName, animatedHand, attackCooldownTicksFloat, upswingRate);
					NetworkHandler.INSTANCE.sendToServer(new PacketWrapper(false, Packets.AttackAnimation.ID, (new Packets.AttackAnimation(this.player.getId(), animatedHand, animationName, attackCooldownTicksFloat, upswingRate)).write()));
//                    ClientPlayNetworking.send(new AttackStaminaCostPacket(((DuckWeaponAttributesAttackMixin) (Object) hand.attack()).bettercombatextension$getStaminaCost()));
					BetterCombatClientEvents.ATTACK_START.invoke((handler) -> {
						handler.onPlayerAttackStart(this.player, hand);
					});
				}
			}
		}
	}

	@Unique
	private void cancelSwingIfNeeded() {
		if (this.upswingStack != null && !areItemStackEqual(this.player.getMainHandItem(), this.upswingStack)) {
			this.cancelWeaponSwing();
		}
	}

	@Unique
	private void attackFromUpswingIfNeeded() {
		if (this.upswingTicks > 0) {
			--this.upswingTicks;
			if (this.upswingTicks == 0) {
				this.performAttack();
				this.upswingStack = null;
			}
		}

	}

	@Unique
	private void resetComboIfNeeded() {
		if (this.lastAttacked > this.comboReset && this.getComboCount() > 0) {
			this.setComboCount(0);
		}

		if (!PlayerAttackHelper.shouldAttackWithOffHand(this.player, this.getComboCount()) && (this.player.getMainHandItem() == null || this.lastAttacedWithItemStack != null && !this.lastAttacedWithItemStack.getItem().equals(this.player.getMainHandItem().getItem()))) {
			this.setComboCount(0);
		}

	}

	@Unique
	private boolean shouldUpdateTargetsInReach() {
		if (!BetterCombatClient.config.isHighlightCrosshairEnabled && !BetterCombatClient.config.isAttackInsteadOfMineWhenEnemiesCloseEnabled) {
			return false;
		} else {
			return this.targetsInReach == null;
		}
	}

	@Unique
	private void updateTargetsInReach(List<Entity> targets) {
		this.targetsInReach = targets;
	}

	@Unique
	private void updateTargetsIfNeeded() {
		if (this.shouldUpdateTargetsInReach()) {
			AttackHand hand = PlayerAttackHelper.getCurrentAttack(this.player, this.getComboCount());
			WeaponAttributes attributes = WeaponRegistry.getAttributes(this.player.getMainHandItem());
			List<Entity> targets = List.of();
			if (attributes != null && attributes.attacks() != null) {
				targets = TargetFinder.findAttackTargets(this.player, this.getCursorTarget(), hand.attack(), attributes.attackRange());
			}

			this.updateTargetsInReach(targets);
		}

	}

	@Inject(
			method = {"tick"},
			at = {@At("HEAD")}
	)
	private void pre_Tick(CallbackInfo ci) {
		if (this.player != null) {
			this.targetsInReach = null;
			++this.lastAttacked;
			this.cancelSwingIfNeeded();
			this.attackFromUpswingIfNeeded();
			this.updateTargetsIfNeeded();
			this.resetComboIfNeeded();
		}
	}

	@Inject(
			method = {"tick"},
			at = {@At("TAIL")}
	)
	private void post_Tick(CallbackInfo ci) {
		if (this.player != null) {
			if (BetterCombatKeybindings.toggleMineKeyBinding.consumeClick()) {
				BetterCombatClient.config.isMiningWithWeaponsEnabled = !BetterCombatClient.config.isMiningWithWeaponsEnabled;
				AutoConfig.getConfigHolder(ClientConfigWrapper.class).save();
				this.textToRender = I18n.get(BetterCombatClient.config.isMiningWithWeaponsEnabled ? "hud.bettercombat.mine_with_weapons_on" : "hud.bettercombat.mine_with_weapons_off", new Object[0]);
				this.textFade = 40;
			}

			if (this.textFade > 0) {
				--this.textFade;
			}

		}
	}

	@Unique
	private void performAttack() {
		ServerConfig serverConfig = BetterCombatExtension.SERVER_CONFIG;
		if (this.player != null) {
			if (BetterCombatKeybindings.feintKeyBinding.isDown()) {
				this.player.resetAttackStrengthTicker();
				this.cancelWeaponSwing();
				// feinting an attack increases combo count
				if (serverConfig.feinting_increases_combo_count.get()) {
					this.setComboCount(this.getComboCount() + 1);
					AttackHand hand = this.getCurrentHand();
					if (hand != null && BetterCombatExtension.isStaminaAttributesLoaded) {
						ExtensionNetworkHandler.sendToServer(new AttackStaminaCostPacket(((DuckLivingEntityMixin) this.player).bettercombatextension$getAttackStaminaCost() * ((DuckWeaponAttributesAttackMixin) (Object) hand.attack()).bettercombatextension$getStaminaCostMultiplier() * serverConfig.global_feint_stamina_cost_multiplier.get()));
					}
				}
			} else {
				AttackHand hand = this.getCurrentHand();
				if (hand != null) {
					WeaponAttributes.Attack attack = hand.attack();
					if (BetterCombatExtension.isStaminaAttributesLoaded) {
						ExtensionNetworkHandler.sendToServer(new AttackStaminaCostPacket(((DuckLivingEntityMixin) this.player).bettercombatextension$getAttackStaminaCost() * ((DuckWeaponAttributesAttackMixin) (Object) attack).bettercombatextension$getStaminaCostMultiplier() * serverConfig.global_attack_stamina_cost_multiplier.get()));
					}
					double upswingRate = hand.upswingRate();
					if (!((double) this.player.getAttackStrengthScale(0.0F) < 1.0 - upswingRate)) {
						Entity cursorTarget = this.getCursorTarget();
						List<Entity> targets = TargetFinder.findAttackTargets(this.player, cursorTarget, attack, hand.attributes().attackRange());
						this.updateTargetsInReach(targets);
						if (targets.size() == 0) {
							PlatformClient.onEmptyLeftClick(this.player);
						}

						NetworkHandler.INSTANCE.sendToServer(new PacketWrapper(false, Packets.C2S_AttackRequest.ID, (new Packets.C2S_AttackRequest(this.getComboCount(), this.player.isShiftKeyDown(), this.player.getInventory().selected, targets)).write()));
						Iterator var7 = targets.iterator();

						while (var7.hasNext()) {
							Entity target = (Entity) var7.next();
							this.player.attack(target);
						}

						this.player.resetAttackStrengthTicker();
						BetterCombatClientEvents.ATTACK_HIT.invoke((handler) -> {
							handler.onPlayerAttackStart(this.player, hand, targets, cursorTarget);
						});
						this.setComboCount(this.getComboCount() + 1);
						if (!hand.isOffHand()) {
							this.lastAttacedWithItemStack = hand.itemStack();
						}

					}
				}
			}
		}
	}

	@Unique
	private AttackHand getCurrentHand() {
		return PlayerAttackHelper.getCurrentAttack(this.player, this.getComboCount());
	}

	@Override
	public InteractionHand bettercombatextension$getCurrentAttackHand() {
		return getCurrentHand() != null && getCurrentHand().isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	@Unique
	private void setComboCount(int comboCount) {
		((PlayerAttackProperties) this.player).setComboCount(comboCount);
	}

	@Unique
	private static boolean areItemStackEqual(ItemStack left, ItemStack right) {
		if (left == null && right == null) {
			return true;
		} else {
			return left != null && right != null ? ItemStack.matches(left, right) : false;
		}
	}

	@Unique
	private void setMiningCooldown(int ticks) {
		Minecraft client = this.thisClient();
		((MinecraftClientAccessor) client).setAttackCooldown(ticks);
	}

	@Unique
	private void cancelWeaponSwing() {
		int downWind = (int) Math.round((double) PlayerAttackHelper.getAttackCooldownTicksCapped(this.player) * (1.0 - 0.5 * (double) BetterCombat.config.upswing_multiplier));
		((PlayerAttackAnimatable) this.player).stopAttackAnimation((float) downWind);
		NetworkHandler.INSTANCE.sendToServer(new PacketWrapper(false, Packets.AttackAnimation.ID, Packets.AttackAnimation.stop(this.player.getId(), downWind).write()));
		this.upswingStack = null;
		this.rightClickDelay = 0;
		this.setMiningCooldown(0);
	}

	public int getComboCount() {
		return ((PlayerAttackProperties) this.player).getComboCount();
	}

	public boolean hasTargetsInReach() {
		return this.targetsInReach != null && !this.targetsInReach.isEmpty();
	}

	public float getSwingProgress() {
		return !((float) this.lastAttacked > this.lastSwingDuration) && !(this.lastSwingDuration <= 0.0F) ? (float) this.lastAttacked / this.lastSwingDuration : 1.0F;
	}

	public int getUpswingTicks() {
		return this.upswingTicks;
	}

	public void cancelUpswing() {
		if (this.upswingTicks > 0) {
			this.cancelWeaponSwing();
		}

	}
}
