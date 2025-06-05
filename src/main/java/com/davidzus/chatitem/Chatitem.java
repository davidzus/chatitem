package com.davidzus.chatitem;

import com.davidzus.chatitem.commands.CommandMod;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chatitem implements ModInitializer {
	public static final String MOD_ID = "chatitem";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandMod.registerCommands();
	}
}