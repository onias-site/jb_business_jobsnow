package com.jb.business.bots.pending.tickets;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the steps of the {@code pendingTickets} command, mapped in the {@code stepFlow} of each step: the
 * operator has no ticket to solve ({@code noPendingTicket}, the session ends), the answer was not understood
 * ({@code invalidAnswer}, the same step asks again) or the operator chose the ticket shown ({@code ticketChosen},
 * the session ends and the bot starts the command of the ticket).
 */
public enum JbSupportPendingTicketsStatus implements CcpProcessStatus{
	/** Status 404: there is no ticket to solve. */
	noPendingTicket(404),
	/** Status 400: the answer was not understood. */
	invalidAnswer(400),
	/** Status 200: the operator chose the ticket shown. */
	ticketChosen(200)
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private JbSupportPendingTicketsStatus(int status) {
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
