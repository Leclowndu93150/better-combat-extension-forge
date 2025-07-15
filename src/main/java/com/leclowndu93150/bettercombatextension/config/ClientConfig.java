package com.leclowndu93150.bettercombatextension.config;

import com.leclowndu93150.bettercombatextension.BetterCombatExtension;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;

public class ClientConfig extends Config {
	public ClientConfig() {
		super(BetterCombatExtension.identifier("client"));
	}
	public ValidatedBoolean enable_poses_while_sprinting = new ValidatedBoolean(true);
}