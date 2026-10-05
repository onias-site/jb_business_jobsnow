package com.jb.business.bots.pending.tickets;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jb.business.bots.engine.JbBotEngineFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;

/**
 * Second step of the {@code pendingTickets} command, repeated until the operator chooses a ticket: {@code 1}
 * solves the ticket shown, {@code 2} shows the next one (after the last one, the first one again). Any other
 * answer shows the same ticket again ({@code invalidAnswer}).
 *
 * <p>Choosing the ticket ends this session ({@code ticketChosen}) and asks the bot to handle the command of the
 * ticket as if the operator had typed it ({@link JbBotEngineFields#typedValueToRedirect}), marked as started from
 * the list ({@link JbBotEngineFields#ticketFromTheList}), so that the list is shown again when the command
 * finishes. The list is read again at every answer: the session may have been left for hours, and in the
 * meantime the operator may have solved tickets by typing their commands. A ticket shown that was solved in the
 * meantime is not run; the operator is told so and sees the next one.
 */
public class JbSupportPendingTicketsChoose implements CcpBusiness {

	/** The answer that solves the ticket shown. */
	private static final String SOLVE = "1";

	/** The answer that shows the next ticket. */
	private static final String NEXT = "2";

	/**
	 * Handles the answer of the operator: solves the ticket shown (redirecting to its command), shows the next one (going
	 * back to the first after the last), or asks again.
	 * @param json the session, with the ticket shown and the answer
	 * @return the session with the reply of the bot
	 * @throws CcpErrorFlowDisturb with {@code noPendingTicket}, {@code ticketChosen} or {@code invalidAnswer}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		List<String> tickets = JbSupportPendingTicketsList.getTickets(json);
		JnLanguage language = JbSupportPendingTicketsList.getLanguage(json);
		boolean noTicket = tickets.isEmpty();

		if(noTicket) {
			CcpJsonRepresentation ended = JbSupportPendingTicketsList.noPendingTicket(json, language);
			return ended;
		}

		String currentTicket = json.getAsString(JbSupportPendingTicketsFields.currentTicket);
		int currentIndex = tickets.indexOf(currentTicket);
		boolean solvedInTheMeantime = currentIndex < 0;
		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String answer = typedValue.trim();

		if(solvedInTheMeantime) {
			String alreadySolved = JbSupportPendingTicketsList.getMessageAboutTheTicket(language, JbSupportPendingTicketsMessages.ticketAlreadySolved, currentTicket);
			CcpJsonRepresentation showingTheFirst = this.showTicket(json, language, tickets, 0, alreadySolved);
			return showingTheFirst;
		}

		boolean solve = SOLVE.equals(answer);

		if(solve) {
			String ticketChosen = JbSupportPendingTicketsList.getMessageAboutTheTicket(language, JbSupportPendingTicketsMessages.ticketChosen, currentTicket);
			CcpJsonRepresentation jsonWithReply = json.put(JbSupportPendingTicketsFields.botReply, ticketChosen);
			CcpJsonRepresentation jsonWithTheCommand = jsonWithReply.put(JbBotEngineFields.typedValueToRedirect, currentTicket);
			CcpJsonRepresentation jsonFromTheList = jsonWithTheCommand.put(JbBotEngineFields.ticketFromTheList, true);
			CcpJsonRepresentation chosen = JbSupportPendingTicketsStatus.ticketChosen.throwException(jsonFromTheList);
			return chosen;
		}

		boolean next = NEXT.equals(answer);

		if(next) {
			int nextIndex = (currentIndex + 1) % tickets.size();
			CcpJsonRepresentation showingTheNext = this.showTicket(json, language, tickets, nextIndex, "");
			return showingTheNext;
		}

		String notUnderstood = JbSupportPendingTicketsMessages.notUnderstood.getMessage(language);
		CcpJsonRepresentation showingTheSame = this.showTicket(json, language, tickets, currentIndex, notUnderstood);
		CcpJsonRepresentation askedAgain = JbSupportPendingTicketsStatus.invalidAnswer.throwException(showingTheSame);
		return askedAgain;
	}

	/**
	 * Puts in the session the reply that shows the ticket at the position, after a notice.
	 * @param json the session
	 * @param language the language of the reply
	 * @param tickets the tickets
	 * @param ticketIndex the position of the ticket
	 * @param notice the notice before the ticket, possibly empty
	 * @return the session with the reply and the ticket shown
	 */
	private CcpJsonRepresentation showTicket(CcpJsonRepresentation json, JnLanguage language, List<String> tickets, int ticketIndex, String notice) {
		String ticketPrompt = JbSupportPendingTicketsList.getTicketPrompt(language, tickets, ticketIndex);
		String reply = notice + ticketPrompt;
		String ticket = tickets.get(ticketIndex);
		CcpJsonRepresentation jsonWithReply = json.put(JbSupportPendingTicketsFields.botReply, reply);
		CcpJsonRepresentation jsonWithTheTicket = jsonWithReply.put(JbSupportPendingTicketsFields.currentTicket, ticket);
		return jsonWithTheTicket;
	}
}
