package com.jb.business.bots.pending.tickets;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Session fields of the {@code pendingTickets} command: {@code currentTicket} is the ticket being shown. The ticket, and
 * not its position, is what the session keeps: the list is read again at every answer, and the tickets solved meanwhile
 * (by another way, or by the operator typing the command) are no longer in it. The text each step sends to the operator
 * is {@code botReply} of {@code JnJsonInstantMessengerFields} (the end message and the flow messages of the steps are just
 * {@code {botReply}}).
 */
public enum JbSupportPendingTicketsFields implements CcpJsonFieldName{
	/** The {@code currentTicket} field. */
	currentTicket
}
