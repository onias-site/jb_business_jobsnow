package com.jb.business.bots.skill.hierarchy;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the steps of the {@code fixSkillHierarchy} command, mapped in the {@code stepFlow} of each step:
 * there is no pending item for the email and the parent ({@code requestNotFound}, the session ends), the answer
 * was not understood ({@code invalidAnswer}, the same step asks again) and every item was decided
 * ({@code reviewFinished}, the session ends with the summary of the review).
 */
public enum JbSupportSkillFixHierarchyStatus implements CcpProcessStatus{
	requestNotFound(404),
	invalidAnswer(400),
	reviewFinished(200)
	;

	public final int status;

	private JbSupportSkillFixHierarchyStatus(int status) {
		this.status = status;
	}

	public int asNumber() {
		return this.status;
	}
}
