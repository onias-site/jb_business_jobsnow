package com.jb.business.bots.engine;

/**
 * Catalogs the commands available in the support bot: {@code solveLoginTokenTicket} (login token tickets),
 * {@code fixSkillHierarchy} (review of the skill hierarchy fix requests), {@code allowCommandToUser} (stops
 * ignoring a user for a command) and {@code pendingTickets} (goes through the commands the bot sent to the
 * operator that were not run yet).
 */
public enum JbSupportBotCommands {

	/** Solves the login token tickets. */
	solveLoginTokenTicket,
	/** Reviews the skill hierarchy fix requests. */
	fixSkillHierarchy,
	/** Stops ignoring a user for a command. */
	allowCommandToUser,
	/** Goes through the pending tickets. */
	pendingTickets
	;
}
