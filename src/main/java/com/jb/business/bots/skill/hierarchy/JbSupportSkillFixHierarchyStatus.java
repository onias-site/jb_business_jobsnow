package com.jb.business.bots.skill.hierarchy;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the steps of the {@code fixSkillHierarchy} command, mapped in the {@code stepFlow} of each step:
 * there is no pending item for the email and the parent ({@code requestNotFound}, the session ends), the answer
 * was not understood ({@code invalidAnswer}, the same step asks again), every item was decided
 * ({@code reviewFinished}, the session ends with the summary of the review), the operator asked to ignore the
 * user ({@code ignoreConfirmationAsked}, goes on to the confirmation), gave up ignoring the user
 * ({@code ignoreCanceled}, back to the choice of how to decide the items) or confirmed it ({@code userIgnored},
 * the session ends), or the user is ignored for the command ({@code userNotAllowed}, the session ends with a
 * notice to the operator).
 */
public enum JbSupportSkillFixHierarchyStatus implements CcpProcessStatus{
	/** Status 404: there is no pending item for the e-mail and the parent. */
	requestNotFound(404),
	/** Status 400: the answer was not understood. */
	invalidAnswer(400),
	/** Status 200: every item was decided. */
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
	private JbSupportSkillFixHierarchyStatus(int status) {
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
