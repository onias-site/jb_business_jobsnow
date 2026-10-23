package com.jb.business.bots.engine;

/**
 * Catalogs the commands of the support bot that belong to jb: {@code pendingTickets} (goes through the commands the bot
 * sent to the operator that were not run yet). The bot also runs the commands other cost centers seed in
 * {@code JnEntityBotCommand} (vis: skill suggestion, skill hierarchy and ignored users; js: login token tickets), which
 * jb does not know: since 2026-10-15 they are found by {@code botName} and their steps run by
 * class name.
 */
public enum JbSupportBotCommands {

	/** Goes through the pending tickets. */
	pendingTickets,
	;
}
