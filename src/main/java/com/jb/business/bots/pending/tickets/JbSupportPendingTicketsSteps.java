package com.jb.business.bots.pending.tickets;

/**
 * Steps of the {@code pendingTickets} command after the first one (the first step has the name of the command
 * itself): the choice between solving the ticket shown and going to the next one, repeated until the operator
 * chooses a ticket.
 */
public enum JbSupportPendingTicketsSteps {
	/** The choice between solving the ticket shown and going to the next one. */
	pendingTicketsChoose
}
