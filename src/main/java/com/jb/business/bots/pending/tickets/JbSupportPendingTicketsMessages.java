package com.jb.business.bots.pending.tickets;

import com.jn.entities.JnEntitySystemMessage;
import com.jn.messages.JnSystemMessage;

/**
 * Texts sent to the support bot operator along the {@code pendingTickets} command, stored in
 * {@link JnEntitySystemMessage} and seeded by {@code JbEntityBot}. The placeholders of the templates are the
 * fields of {@link JbSupportPendingTicketsMessageFields}.
 */
public enum JbSupportPendingTicketsMessages implements JnSystemMessage {

	/** Header of the list when there is a single ticket. */
	oneTicket,

	/** Header of the list when there are many tickets: {@code {ticketsCount}}. */
	manyTickets,

	/**
	 * Shows one ticket and asks whether to solve it or go to the next one: {@code {ticketNumber}},
	 * {@code {ticketsCount}} and {@code {ticket}}.
	 */
	ticketPrompt,

	/** The operator has no ticket to solve. */
	noPendingTicket,

	/** Answer that is neither of the options; precedes the ticket shown again. */
	notUnderstood,

	/** The ticket shown was solved in the meantime: {@code {ticket}}; precedes the next ticket. */
	ticketAlreadySolved,

	/** The operator chose the ticket, whose command starts right after: {@code {ticket}}. */
	ticketChosen,
	;
}
