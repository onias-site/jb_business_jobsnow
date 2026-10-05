package com.jb.business.bots.pending.tickets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.query.CcpQueryExecutorDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jb.entities.JbEntityPendingTickets;
import com.jn.entities.JnEntitySupportPendingCommand;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;

/**
 * The tickets of the operator and the texts that show them, shared by the steps of the {@code pendingTickets}
 * command.
 *
 * <p>Reading the list first moves the commands that reached the operator since the last reading from the inbox
 * written by the message sending ({@link JnEntitySupportPendingCommand}) to {@link JbEntityPendingTickets}. Both
 * are found by a query, which Elasticsearch answers from what was refreshed up to a second ago, so each record
 * found is confirmed by its id, which is read in real time: without that, a ticket solved a moment ago would
 * still be listed, or would even come back from the inbox.
 */
final class JbSupportPendingTicketsList {

	/** Utility class; not instantiable. */
	private JbSupportPendingTicketsList() {}

	/**
	 * The tickets of the operator of the session, oldest first.
	 */
	static List<String> getTickets(CcpJsonRepresentation json) {

		String botName = json.getAsString(JnJsonInstantMessengerFields.botName);
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);

		List<CcpJsonRepresentation> movedTickets = moveTheInbox(botName, chatId);
		List<CcpJsonRepresentation> listedTickets = findExisting(JbEntityPendingTickets.ENTITY, botName, chatId, JbEntityPendingTickets.Fields.botName, JbEntityPendingTickets.Fields.chatId);

		// a ticket just moved may not be in the query yet, and one sent twice is a single ticket
		Map<String, CcpJsonRepresentation> ticketsByCommand = new LinkedHashMap<>();
		Stream<CcpJsonRepresentation> allTicketsStream = Stream.concat(listedTickets.stream(), movedTickets.stream());
		allTicketsStream.forEach(ticket -> ticketsByCommand.putIfAbsent(ticket.getAsString(JbEntityPendingTickets.Fields.ticket), ticket));

		Collection<CcpJsonRepresentation> distinctTickets = ticketsByCommand.values();
		Stream<CcpJsonRepresentation> ticketsStream = distinctTickets.stream();
		Comparator<CcpJsonRepresentation> byTimestamp = Comparator.comparing(ticket -> ticket.getAsLongNumber(JbEntityPendingTickets.Fields.timestamp));
		Comparator<CcpJsonRepresentation> byTimestampAndTicket = byTimestamp.thenComparing(ticket -> ticket.getAsString(JbEntityPendingTickets.Fields.ticket));
		Stream<CcpJsonRepresentation> sortedTicketsStream = ticketsStream.sorted(byTimestampAndTicket);
		Stream<String> ticketCommandsStream = sortedTicketsStream.map(ticket -> ticket.getAsString(JbEntityPendingTickets.Fields.ticket));
		List<String> tickets = ticketCommandsStream.collect(Collectors.toList());
		return tickets;
	}

	/**
	 * Moves the commands of the inbox of the operator ({@code JnEntitySupportPendingCommand}) to the pending tickets.
	 * @param botName the bot
	 * @param chatId the chat of the operator
	 * @return the tickets moved
	 */
	private static List<CcpJsonRepresentation> moveTheInbox(String botName, Long chatId) {

		List<CcpJsonRepresentation> inbox = findExisting(JnEntitySupportPendingCommand.ENTITY, botName, chatId, JnEntitySupportPendingCommand.Fields.botName, JnEntitySupportPendingCommand.Fields.chatId);
		List<CcpJsonRepresentation> movedTickets = new ArrayList<>();

		for (CcpJsonRepresentation pendingCommand : inbox) {
			String command = pendingCommand.getAsString(JnEntitySupportPendingCommand.Fields.command);
			Long timestamp = pendingCommand.getAsLongNumber(JnEntitySupportPendingCommand.Fields.timestamp);

			CcpJsonRepresentation ticketWithBotName = CcpOtherConstants.EMPTY_JSON.put(JbEntityPendingTickets.Fields.botName, botName);
			CcpJsonRepresentation ticketWithChatId = ticketWithBotName.put(JbEntityPendingTickets.Fields.chatId, chatId);
			CcpJsonRepresentation ticketWithCommand = ticketWithChatId.put(JbEntityPendingTickets.Fields.ticket, command);
			CcpJsonRepresentation ticket = ticketWithCommand.put(JbEntityPendingTickets.Fields.timestamp, timestamp);

			JbEntityPendingTickets.ENTITY.save(ticket);
			JnEntitySupportPendingCommand.ENTITY.delete(pendingCommand);
			movedTickets.add(ticket);
		}

		return movedTickets;
	}

	/**
	 * The records of the operator in the entity, each one confirmed by its id.
	 */
	private static List<CcpJsonRepresentation> findExisting(CcpEntity entity, String botName, Long chatId, CcpJsonFieldName botNameField, CcpJsonFieldName chatIdField) {

		var query = CcpQueryOptions.INSTANCE
				.startQuery();
		var bool = query
				.startBool();
		var mustWithTheBotName = bool
				.startMust()
				.term(botNameField, botName);
		var mustWithTheChatId = mustWithTheBotName
				.term(chatIdField, chatId);
		var boolWithTheOperator = mustWithTheChatId
				.endMustAndBackToBool();
		var queryWithTheOperator = boolWithTheOperator
				.endBoolAndBackToQuery();
		CcpQueryOptions requestWithTheOperator = queryWithTheOperator
				.endQueryAndBackToRequest();
		CcpQueryOptions requestWithAllResults = requestWithTheOperator.maxResults();

		CcpQueryExecutorDecorator recordsOfTheOperator = requestWithAllResults.selectFrom(entity);
		List<CcpJsonRepresentation> resultList = recordsOfTheOperator.getResultAsList();
		// the chatId comes back from the database as a double (7.51717896E8), and the id is calculated over the
		// text of the values of the primary key: without the long, no record would be found by its id
		Stream<CcpJsonRepresentation> resultStream = resultList.stream();
		Stream<CcpJsonRepresentation> foundRecordsStream = resultStream.map(record -> record.put(chatIdField, chatId));
		List<CcpJsonRepresentation> foundRecords = foundRecordsStream.collect(Collectors.toList());
		boolean nothingFound = foundRecords.isEmpty();

		if(nothingFound) {
			return foundRecords;
		}

		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpJsonRepresentation[] foundRecordsArray = foundRecords.toArray(new CcpJsonRepresentation[foundRecords.size()]);
		CcpSelectUnionAll recordsById = crud.unionAll(foundRecordsArray, JnDeleteKeysFromCache.INSTANCE, entity);
		Stream<CcpJsonRepresentation> recordsToConfirmStream = foundRecords.stream();
		Stream<CcpJsonRepresentation> existingRecordsStream = recordsToConfirmStream.filter(record -> entity.isPresentInThisUnionAll(recordsById, record));
		List<CcpJsonRepresentation> existingRecords = existingRecordsStream.collect(Collectors.toList());
		return existingRecords;
	}

	/**
	 * Returns the language of the session (the enum itself or its name), Portuguese by default.
	 * @param json the session
	 * @return the language
	 */
	static JnLanguage getLanguage(CcpJsonRepresentation json) {
		// the language may be in the json as the enum itself or as its name
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		return language;
	}

	/**
	 * How many tickets the operator has, followed by the first one.
	 */
	static String getListText(JnLanguage language, List<String> tickets) {
		int ticketsCount = tickets.size();
		boolean singleTicket = ticketsCount == 1;
		JbSupportPendingTicketsMessages header = singleTicket ? JbSupportPendingTicketsMessages.oneTicket : JbSupportPendingTicketsMessages.manyTickets;
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportPendingTicketsMessageFields.ticketsCount, ticketsCount);
		String headerText = header.getMessage(language, parameters);
		String firstTicketPrompt = getTicketPrompt(language, tickets, 0);
		String listText = headerText + "\n" + firstTicketPrompt;
		return listText;
	}

	/**
	 * Shows the ticket at the given position and asks whether to solve it or go to the next one.
	 */
	static String getTicketPrompt(JnLanguage language, List<String> tickets, int ticketIndex) {
		int ticketNumber = ticketIndex + 1;
		int ticketsCount = tickets.size();
		String ticket = tickets.get(ticketIndex);

		CcpJsonRepresentation parametersWithNumber = CcpOtherConstants.EMPTY_JSON.put(JbSupportPendingTicketsMessageFields.ticketNumber, ticketNumber);
		CcpJsonRepresentation parametersWithCount = parametersWithNumber.put(JbSupportPendingTicketsMessageFields.ticketsCount, ticketsCount);
		CcpJsonRepresentation parameters = parametersWithCount.put(JbSupportPendingTicketsMessageFields.ticket, ticket);

		String ticketPrompt = JbSupportPendingTicketsMessages.ticketPrompt.getMessage(language, parameters);
		return ticketPrompt;
	}

	/**
	 * Returns a message about the ticket in the language.
	 * @param language the language
	 * @param message the message
	 * @param ticket the ticket
	 * @return the message
	 */
	static String getMessageAboutTheTicket(JnLanguage language, JbSupportPendingTicketsMessages message, String ticket) {
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportPendingTicketsMessageFields.ticket, ticket);
		String messageAboutTheTicket = message.getMessage(language, parameters);
		return messageAboutTheTicket;
	}

	/**
	 * Ends the session telling the operator that there is no ticket to solve.
	 */
	static CcpJsonRepresentation noPendingTicket(CcpJsonRepresentation json, JnLanguage language) {
		String noPendingTicket = JbSupportPendingTicketsMessages.noPendingTicket.getMessage(language);
		CcpJsonRepresentation jsonWithReply = json.put(JbSupportPendingTicketsFields.botReply, noPendingTicket);
		CcpJsonRepresentation ended = JbSupportPendingTicketsStatus.noPendingTicket.throwException(jsonWithReply);
		return ended;
	}
}
