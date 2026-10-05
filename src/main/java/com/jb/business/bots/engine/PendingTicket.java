package com.jb.business.bots.engine;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jb.entities.JbEntityPendingTickets;
import com.jn.entities.JnEntitySupportPendingCommand;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * What happens to a ticket of the operator when the command that solves it finishes: it leaves the list of
 * pending tickets ({@link JbEntityPendingTickets}) and the inbox it may not have left yet
 * ({@link JnEntitySupportPendingCommand}). This holds whether the command was started from the
 * {@code /pendingTickets} list or typed by the operator on their own, since the ticket is identified by the
 * command itself ({@link JbBotEngineFields#pendingTicket}).
 *
 * <p>A command finishes when its session ends without an unforeseen error: the last step succeeded, or a step
 * diverted the flow to an end foreseen in its {@code stepFlow} (for example, there was no open ticket for the
 * email, which also means there is nothing left to solve). A command left in the middle, waiting for an answer,
 * keeps its ticket.
 */
final class PendingTicket {

	/** Utility class; not instantiable. */
	private PendingTicket() {}

	/**
	 * Closes the ticket the command solved (from {@code pendingTicket}) and, when the command was started from the list,
	 * redirects to {@code /pendingTickets} to show the list again.
	 * @param stepInput what arrived at the step
	 * @param result the result of the step
	 * @return the result, possibly with the redirection
	 */
	static CcpJsonRepresentation commandFinished(CcpJsonRepresentation stepInput, CcpJsonRepresentation result) {

		String ticket = stepInput.getAsString(JbBotEngineFields.pendingTicket);
		boolean noTicket = ticket.isEmpty();

		if(noTicket) {
			return result;
		}

		String botName = stepInput.getAsString(JnJsonInstantMessengerFields.botName);
		Long chatId = stepInput.getAsLongNumber(JnJsonInstantMessengerFields.chatId);

		CcpJsonRepresentation ticketWithBotName = CcpOtherConstants.EMPTY_JSON.put(JbEntityPendingTickets.Fields.botName, botName);
		CcpJsonRepresentation ticketWithChatId = ticketWithBotName.put(JbEntityPendingTickets.Fields.chatId, chatId);
		CcpJsonRepresentation pendingTicket = ticketWithChatId.put(JbEntityPendingTickets.Fields.ticket, ticket);
		CcpJsonRepresentation pendingCommand = ticketWithChatId.put(JnEntitySupportPendingCommand.Fields.command, ticket);

		JbEntityPendingTickets.ENTITY.delete(pendingTicket);
		JnEntitySupportPendingCommand.ENTITY.delete(pendingCommand);

		boolean startedFromTheList = stepInput.getAsBoolean(JbBotEngineFields.ticketFromTheList);

		if(false == startedFromTheList) {
			return result;
		}

		// the list is shown again, without the ticket just solved; the flag is not passed on, otherwise the end of
		// the list would show the list again
		String pendingTicketsCommand = "/" + JbSupportBotCommands.pendingTickets;
		CcpJsonRepresentation resultWithoutTheFlag = result.removeFields(JbBotEngineFields.ticketFromTheList);
		CcpJsonRepresentation resultShowingTheListAgain = resultWithoutTheFlag.put(JbBotEngineFields.typedValueToRedirect, pendingTicketsCommand);
		return resultShowingTheListAgain;
	}
}
