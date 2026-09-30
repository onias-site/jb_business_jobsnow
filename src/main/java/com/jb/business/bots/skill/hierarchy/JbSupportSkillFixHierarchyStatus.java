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
	requestNotFound(404),
	invalidAnswer(400),
	reviewFinished(200),
	userIgnored(201),
	ignoreConfirmationAsked(202),
	ignoreCanceled(205),
	userNotAllowed(403)
	;

	public final int status;

	private JbSupportSkillFixHierarchyStatus(int status) {
		this.status = status;
	}

	public int asNumber() {
		return this.status;
	}
}
