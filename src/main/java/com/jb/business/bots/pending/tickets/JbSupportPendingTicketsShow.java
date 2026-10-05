package com.jb.business.bots.pending.tickets;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;

/**
 * First step of the {@code pendingTickets} command ({@code /pendingTickets}): tells the operator how many tickets
 * they have to solve and shows the oldest one, asking whether to solve it or go to the next one. Without any
 * ticket the session ends with that notice ({@code noPendingTicket}).
 */
public class JbSupportPendingTicketsShow implements CcpBusiness {

	/**
	 * Shows the count and the oldest ticket.
	 * @param json the session
	 * @return the session with the reply and the ticket shown
	 * @throws CcpErrorFlowDisturb with {@code noPendingTicket} when there is no ticket
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		List<String> tickets = JbSupportPendingTicketsList.getTickets(json);
		JnLanguage language = JbSupportPendingTicketsList.getLanguage(json);
		boolean noTicket = tickets.isEmpty();

		if(noTicket) {
			CcpJsonRepresentation ended = JbSupportPendingTicketsList.noPendingTicket(json, language);
			return ended;
		}

		String listText = JbSupportPendingTicketsList.getListText(language, tickets);
		String firstTicket = tickets.get(0);
		CcpJsonRepresentation jsonWithReply = json.put(JbSupportPendingTicketsFields.botReply, listText);
		CcpJsonRepresentation jsonWithTheTicket = jsonWithReply.put(JbSupportPendingTicketsFields.currentTicket, firstTicket);
		return jsonWithTheTicket;
	}
}
