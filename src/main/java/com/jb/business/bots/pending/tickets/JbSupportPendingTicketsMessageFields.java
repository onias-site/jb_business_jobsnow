package com.jb.business.bots.pending.tickets;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Placeholders of the texts of {@link JbSupportPendingTicketsMessages}: how many tickets there are
 * ({@code ticketsCount}), the position of the ticket shown ({@code ticketNumber}) and the ticket itself
 * ({@code ticket}).
 */
public enum JbSupportPendingTicketsMessageFields implements CcpJsonFieldName{
	/** The {@code ticketsCount} field. */
	ticketsCount,
	/** The {@code ticketNumber} field. */
	ticketNumber,
	/** The {@code ticket} field. */
	ticket
}
