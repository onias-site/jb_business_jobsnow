package com.jb.business.bots.engine;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Session fields the engine itself handles, whatever the command:
 * <ul>
 * <li>{@code pendingTicket}: the command typed, as it identifies a ticket of the operator (the canonical name of
 * the command followed by its parameters). It is put when the command starts and, when the command finishes, the
 * ticket leaves the list of pending tickets.</li>
 * <li>{@code ticketFromTheList}: the command was started from the {@code /pendingTickets} list, so when it
 * finishes the list is shown again.</li>
 * <li>{@code typedValueToRedirect}: text a step asks the bot to handle next, as if the operator had typed it; this
 * is how a step hands the conversation over to another command.</li>
 * </ul>
 */
public enum JbBotEngineFields implements CcpJsonFieldName{
	/** The {@code pendingTicket} field. */
	pendingTicket,
	/** The {@code ticketFromTheList} field. */
	ticketFromTheList,
	/** The {@code typedValueToRedirect} field. */
	typedValueToRedirect
}
