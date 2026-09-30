package com.jb.business.bots.command.allowed;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Parameter of the {@code allowCommandToUser} command ({@code /allowCommandToUser <command> <email>}) besides
 * the email: the name of the command the user was ignored for. It is not called {@code commandName} because
 * the bot session already keeps, under that name, the command being run.
 */
public enum JbSupportAllowCommandToUserFields implements CcpJsonFieldName{
	command
}
