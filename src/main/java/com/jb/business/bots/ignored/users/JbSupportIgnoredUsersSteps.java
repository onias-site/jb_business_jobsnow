package com.jb.business.bots.ignored.users;

/**
 * Steps of the {@code ignoredUsers} command after the first one (the first step has the name of the command itself):
 * the choice between stopping ignoring the user shown and going to the next one, repeated until the operator chooses
 * a user.
 */
public enum JbSupportIgnoredUsersSteps {
	/** The choice between stopping ignoring the user shown and going to the next one. */
	ignoredUsersChoose
}
