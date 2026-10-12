package com.jb.business.bots.ignored.users;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Placeholders of the texts of {@link JbSupportIgnoredUsersMessages}: how many users are ignored
 * ({@code usersCount}), the position of the user shown ({@code userNumber}), their email ({@code email}), the command
 * in which the operator ignored them ({@code commandName}) and the request that led to it ({@code request}).
 */
public enum JbSupportIgnoredUsersMessageFields implements CcpJsonFieldName{
	/** The {@code usersCount} field. */
	usersCount,
	/** The {@code userNumber} field. */
	userNumber,
	/** The {@code email} field. */
	email,
	/** The {@code commandName} field. */
	commandName,
	/** The {@code request} field. */
	request
}
