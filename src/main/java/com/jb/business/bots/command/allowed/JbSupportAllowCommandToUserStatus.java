package com.jb.business.bots.command.allowed;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversion of the {@code allowCommandToUser} command, mapped in its {@code stepFlow}: the user was not
 * ignored for the command ({@code userNotIgnored}), the session ends with a notice to the operator.
 */
public enum JbSupportAllowCommandToUserStatus implements CcpProcessStatus{
	userNotIgnored(404)
	;

	public final int status;

	private JbSupportAllowCommandToUserStatus(int status) {
		this.status = status;
	}

	public int asNumber() {
		return this.status;
	}
}
