package com.jb.business.bots.ignored.users;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Session fields of the {@code ignoredUsers} command: {@code botReply} is the text each step sends to the operator
 * (the end message and the flow messages of the steps are just {@code {botReply}}) and {@code currentUser} is the
 * email of the user being shown. The email, and not its position, is what the session keeps: the list is read again
 * at every answer, and the users the operator stopped ignoring meanwhile are no longer in it.
 */
public enum JbSupportIgnoredUsersFields implements CcpJsonFieldName{
	/** The {@code botReply} field. */
	botReply,
	/** The {@code currentUser} field. */
	currentUser
}
