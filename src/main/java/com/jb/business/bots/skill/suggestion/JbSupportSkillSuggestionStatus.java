package com.jb.business.bots.skill.suggestion;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the steps of the {@code reviewSkillSuggestion} command, mapped in the {@code stepFlow} of each step:
 * there is no pending suggestion for the email and the skill ({@code requestNotFound}, the session ends), the answer
 * was not understood ({@code invalidAnswer}, the same step asks again), the suggestion was decided
 * ({@code reviewFinished}, the session ends with the summary), the operator asked to ignore the user
 * ({@code ignoreConfirmationAsked}, goes on to the confirmation), gave up ignoring the user ({@code ignoreCanceled},
 * back to the decision) or confirmed it ({@code userIgnored}, the session ends), or the user is ignored for the
 * command ({@code userNotAllowed}, the session ends with a notice to the operator).
 */
public enum JbSupportSkillSuggestionStatus implements CcpProcessStatus{
	/** Status 404: there is no pending suggestion for the e-mail and the skill. */
	requestNotFound(404),
	/** Status 400: the answer was not understood. */
	invalidAnswer(400),
	/** Status 200: the suggestion was decided. */
	reviewFinished(200),
	/** Status 201: the user is now ignored for the command. */
	userIgnored(201),
	/** Status 202: the operator asked to ignore the user; the confirmation follows. */
	ignoreConfirmationAsked(202),
	/** Status 205: the operator gave up ignoring the user. */
	ignoreCanceled(205),
	/** Status 403: the user is ignored for the command. */
	userNotAllowed(403)
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private JbSupportSkillSuggestionStatus(int status) {
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
