package com.jb.business.bots.engine;

/**
 * Catalogs the commands available in the support bot: {@code solveLoginTokenTicket} (login token tickets),
 * {@code fixSkillHierarchy} (review of the skill hierarchy fix requests) and {@code allowCommandToUser} (stops
 * ignoring a user for a command).
 */
public enum JbSupportBotCommands {

	solveLoginTokenTicket, 
	fixSkillHierarchy,
	allowCommandToUser
	;
}
