package com.jb.business.bots.command.allowed;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversion of the {@code allowCommandToUser} command, mapped in its {@code stepFlow}: the user was not
 * ignored for the command ({@code userNotIgnored}), the session ends with a notice to the operator.
 */
public enum JbSupportAllowCommandToUserStatus implements CcpProcessStatus{
	/** Status 404: the user was not ignored for the command. */
	userNotIgnored(404)
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private JbSupportAllowCommandToUserStatus(int status) {
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
