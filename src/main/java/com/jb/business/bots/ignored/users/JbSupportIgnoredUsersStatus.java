package com.jb.business.bots.ignored.users;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the steps of the {@code ignoredUsers} command, mapped in the {@code stepFlow} of each step: no user
 * is ignored ({@code noIgnoredUser}, the session ends), the answer was not understood ({@code invalidAnswer}, the same
 * step asks again) or the operator chose to stop ignoring the user shown ({@code userChosen}, the session ends and the
 * bot starts {@code /allowCommandToUser} for the user).
 */
public enum JbSupportIgnoredUsersStatus implements CcpProcessStatus{
	/** Status 404: no user is ignored. */
	noIgnoredUser(404),
	/** Status 400: the answer was not understood. */
	invalidAnswer(400),
	/** Status 200: the operator chose to stop ignoring the user shown. */
	userChosen(200)
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private JbSupportIgnoredUsersStatus(int status) {
		this.status = status;
	}

	/**
	 * Returns the HTTP status code.
	 * @return the status code
	 */
	public int asNumber() {
		return this.status;
	}
}
