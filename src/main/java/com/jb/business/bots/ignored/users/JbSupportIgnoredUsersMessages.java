package com.jb.business.bots.ignored.users;

import com.jn.messages.JnSystemMessage;

/**
 * Texts sent to the support bot operator along the {@code ignoredUsers} command, stored in
 * {@code JnEntitySystemMessage} and seeded by {@code JbEntityBot}. The placeholders of the templates are the fields of
 * {@link JbSupportIgnoredUsersMessageFields}.
 */
public enum JbSupportIgnoredUsersMessages implements JnSystemMessage {

	/** Header of the list when a single user is ignored. */
	oneUser,

	/** Header of the list when many users are ignored: {@code {usersCount}}. */
	manyUsers,

	/**
	 * Shows one ignored user and asks whether to stop ignoring them or go to the next one: {@code {userNumber}},
	 * {@code {usersCount}}, {@code {email}}, {@code {commandName}} and {@code {request}}.
	 */
	userPrompt,

	/** No user is ignored. */
	noIgnoredUser,

	/** Answer that is neither of the options; precedes the user shown again. */
	notUnderstood,

	/** The user shown is no longer ignored (the operator allowed them in the meantime): {@code {email}}; precedes the next user. */
	userAlreadyAllowed,

	/** The operator chose to stop ignoring the user, which the {@code allowCommandToUser} command does right after: {@code {email}}. */
	userChosen,
	;
}
