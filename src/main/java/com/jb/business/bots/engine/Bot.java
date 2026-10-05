package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jb.entities.JbEntityBot;
import com.jb.entities.JbEntityBotAllowedUser;
import com.jb.entities.JbEntityBotCommandStepSession;
import com.jb.entities.JbEntityBotExplanation;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;

import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * A bot of the platform, loaded from the database: its explanations by language, its commands (the configured ones plus
 * the default ones) and, for a restricted bot, the chat ids allowed to use it. It receives each message, loads or starts
 * the session of the chat and runs the step of the command.
 */
class Bot implements JbBotBusiness{
	/** The type of the bot. */
	private final JbBotType botType;
	/** Whether only the allowed users may use the bot. */
	private final boolean isRestricted;
	/** The names of the commands of the bot. */
	private final List<String> commands;
	/** The chat ids allowed to use a restricted bot. */
	private final Set<Long> allowedUsers;
	/** The explanations of the bot, by language. */
	private final List<CcpJsonRepresentation> explanations;

	/**
	 * Loads the bot from the result of the search of the bot entities.
	 * @param botType the type of the bot
	 * @param resultFromSearchBots the search result
	 */
	Bot(JbBotType botType, CcpSelectUnionAll resultFromSearchBots) {
		String botTypeName = botType.name();
		this.explanations = this.loadLabelsWithLanguages(botTypeName, resultFromSearchBots, JbEntityBotExplanation.ENTITY, JnJsonInstantMessengerFields.botName, JnJsonCommonsFields.language, JnJsonInstantMessengerFields.message);
		this.allowedUsers = this.loadAllowedUsers(botType, resultFromSearchBots);
		this.commands = this.loadCommands(botType, resultFromSearchBots);
		this.isRestricted = botType.isRestricted();
		this.botType = botType;
		
	}
	
	/**
	 * Reads the allowed chat ids (stored as text, possibly in scientific notation).
	 * @param valueOf the type of the bot
	 * @param resultFromSearchBots the search result
	 * @return the allowed chat ids
	 */
	private Set<Long> loadAllowedUsers(JbBotType valueOf, CcpSelectUnionAll resultFromSearchBots) {

		Supplier<CcpJsonRepresentation> parameterToSearchBot = valueOf.getParameterToSearchBot();
		CcpJsonRepresentation recordFromUnionAll = JbEntityBotAllowedUser.ENTITY.getRecordFromUnionAll(resultFromSearchBots, parameterToSearchBot);
		List<String> asStringList = recordFromUnionAll.getAsStringList(JbEntityBotAllowedUser.Fields.allowedUser);
		Stream<String> stream = asStringList.stream();
		var streamMap = stream.map(x -> Double.valueOf(x).longValue());
		Set<Long> collect = streamMap.collect(Collectors.toSet());
		return collect;
	}

	/**
	 * Reads the configured commands and adds the default ones.
	 * @param valueOf the type of the bot
	 * @param resultFromSearchBots the search result
	 * @return the command names
	 */
	private List<String> loadCommands(JbBotType valueOf, CcpSelectUnionAll resultFromSearchBots){
		Supplier<CcpJsonRepresentation> jsonSupplier = valueOf.getParameterToSearchBot();
		CcpJsonRepresentation recordFromUnionAll = JbEntityBot.ENTITY.getRecordFromUnionAll(resultFromSearchBots, jsonSupplier);
		List<String> asStringList2 = recordFromUnionAll.getAsStringList(JnJsonInstantMessengerFields.commandName);
		List<String> commands = new ArrayList<>(asStringList2);
		JbDefaultBotCommandStep[] values = JbDefaultBotCommandStep.values();
		Stream<JbDefaultBotCommandStep> stream2 = Arrays.asList(values).stream();
		var stream2Map = stream2.map(x -> x.name());
		List<String> commonsCommands = stream2Map.collect(Collectors.toList());
		commands.addAll(commonsCommands);
		return commands;
	}
	
	/**
	 * Returns the name of the bot.
	 * @return the name
	 */
	public String toString() {
		String name = this.name();
		return name;
	}
	
	
	/**
	 * Sends the message of the current step in the language of the session, when there is one.
	 * @param json the session
	 * @param messages the messages of the steps, by language
	 * @return the result of the sending, or the session when there is no message
	 */
	CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, List<CcpJsonRepresentation> messages) {
		
		String language = json.getAsString(JnJsonCommonsFields.language);
		String stepName = json.getAsString(JnJsonInstantMessengerFields.stepName);
		Stream<CcpJsonRepresentation> stream3 = messages.stream();
		Stream<CcpJsonRepresentation> filter = stream3
				.filter(x -> x.getAsString(JnJsonInstantMessengerFields.stepName).equals(stepName));
				var filter2 = filter
				.filter(x -> x.getAsString(JnJsonCommonsFields.language).equals(language));

				Optional<CcpJsonRepresentation> findFirst = filter2
				.findFirst();
				boolean findFirstPresent = findFirst.isPresent();

				boolean hasNoMessages = false == findFirstPresent;
		
		if(hasNoMessages) {
			return json;
		}
		
		CcpJsonRepresentation message = findFirst.get();

		CcpJsonRepresentation sendMessage = this.sendMessage(json, message);
		
		return sendMessage;
		
	}

	/**
	 * Sends one message, with the bot token, as the message type it names ({@code text} by default).
	 * @param json the session
	 * @param message the message
	 * @return the session merged with the answer of the provider
	 */
	protected CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, CcpJsonRepresentation message) {
		String type = message.getOrDefault(JnJsonInstantMessengerFields.instantMessageType, () -> JnInstantMessageType.text.name());
		
		CcpJsonRepresentation putToken = this.putToken(json);
		JnInstantMessageType valueOf = JnInstantMessageType.valueOf(type);
		CcpJsonRepresentation sendMessage = valueOf.sendMessage(putToken, message);
		return sendMessage;
	}
	
	/**
	 * Loads the session of the chat. Typing a command starts it over (a priority command returns at once); otherwise the
	 * session saved in the database is read, or a new one is created. What the user has just typed, the reply id and the
	 * chat id prevail over the saved session.
	 * @param json the message
	 * @return the session
	 */
	public CcpJsonRepresentation loadSession(CcpJsonRepresentation json) {
		
		Collection<BotCommand> allCommands = JbBotEngine.INSTANCE.allCommands.values();
		
		for (BotCommand command : allCommands) {
			
			boolean commandNameDoesNotMatch = command.commandNameDoesNotMatch(json);
			
			if(commandNameDoesNotMatch) {
				continue;
			}
			boolean visible = command.isVisible(json);

			boolean invisibleCommand = false == visible;
			
			if(invisibleCommand) {
				continue;
			}
			
			boolean hasPriority = command.hasPriority(json);
			
			if(hasPriority) {
				CcpJsonRepresentation priorityCommand = command.getCommandJson(json);
				return priorityCommand;
			}
			// typing the command itself starts it over: a session left in the middle of a multi-step command (in
			// memory or in the database) is discarded, otherwise the user would be sent back to the old step and
			// the command just typed, with its parameters, would be lost
			json = command.getCommandJson(json);
			JbDefaultBotCommandStep.removeSession.execute(json);
			break;
		}
		
		

		CcpEntityMetaData entityMetaData = JbEntityBotCommandStepSession.ENTITY.getEntityMetaData();
		
		CcpBusiness newSessionProducer = this.newSessionProducer(json);
		
		CcpJsonRepresentation savedSession = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(json, newSessionProducer);
		CcpJsonRepresentation innerJson = savedSession.getInnerJson(JnJsonCommonsFields.json);
		CcpJsonRepresentation removeFields = savedSession.removeFields(JnJsonCommonsFields.json);
		CcpJsonRepresentation handledSession = innerJson.mergeWithAnotherJson(removeFields);

		// what the user has just typed prevails over the session: the saved session still carries the text typed
		// in the previous step, and without this the next step of a multi-step command would read that old text.
		// The chatId of the message prevails too: the one read back from the database is a double (7.51717896E8),
		// and the session id, calculated over it, would not be the one the next message looks for
		CcpJsonRepresentation justTyped = json.getJsonPiece(JnJsonCommonsFields.typedValue, CcpJsonCommonsFields.replyTo, JnJsonInstantMessengerFields.chatId);
		CcpJsonRepresentation sessionWithWhatWasJustTyped = handledSession.mergeWithAnotherJson(justTyped);

		return sessionWithWhatWasJustTyped;
	}

	/**
	 * Tells whether the user may use the bot.
	 * @param json the message
	 * @return {@code true} for an open bot or an allowed user
	 */
	public boolean isVisible(CcpJsonRepresentation json) {
		
		boolean openBot = false == this.isRestricted;
		if(openBot) {
			return true;
		}
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);

		boolean alloedUser = this.allowedUsers.contains(chatId);
		
		return alloedUser;
	}
	
	/**
	 * Builds the producer of a new session: the typed text, Portuguese as the language and the command name when there is
	 * no session.
	 * @param json the message
	 * @return the producer
	 */
	private CcpBusiness newSessionProducer(CcpJsonRepresentation json) {
		CcpBusiness newSessionProducer = jsn -> 
		json
		.mergeWithAnotherJson(json)
		.renameField(JnJsonInstantMessengerFields.message, JnJsonCommonsFields.typedValue)
		.put(JnJsonCommonsFields.language, JnLanguage.portuguese)
		.getTransformedJson(JsonProducers.putCommandNameWhenHasNoSession)
		;
		return newSessionProducer;
	}

	/**
	 * Handles a message: loads the session, runs the step of the command and follows a redirection the step asked for.
	 * @param message the message received
	 * @return the result of the step
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation message) {
		String botTypeName2 = botType.name();
		CcpJsonRepresentation put = message
				.put(JnJsonInstantMessengerFields.botName, botTypeName2);
				CcpJsonRepresentation put2 = put
				//LATER PARAMETERIZE THIS TEXT
				.put(JnJsonInstantMessengerFields.instantMessageType, JnInstantMessageType.text);
				CcpJsonRepresentation json = put2
				.renameField(JbBotEngine.Fields.message_id, CcpJsonCommonsFields.replyTo)
				;

		CcpJsonRepresentation renameField = json.renameField(JnJsonInstantMessengerFields.message, JnJsonCommonsFields.typedValue);
		CcpJsonRepresentation loadSession = this.loadSession(renameField);
		BotCommand botCommand = this.getCommand(loadSession);
		CcpJsonRepresentation execute = botCommand.execute(loadSession);
		CcpJsonRepresentation redirected = this.redirect(message, execute);
		return redirected;
	}

	/**
	 * When the step left a text in {@link JbBotEngineFields#typedValueToRedirect}, the bot handles it next, as if
	 * the operator had typed it in the same message: choosing a ticket in the {@code /pendingTickets} list starts
	 * the command of the ticket, and finishing that command shows the list again. The session of the step that
	 * redirected has already ended, so the text starts its command from the beginning. The
	 * {@link JbBotEngineFields#ticketFromTheList} flag goes along only when the step asked for it. A redirection to
	 * the very text just handled is ignored, so that a step cannot put the bot in a loop.
	 */
	private CcpJsonRepresentation redirect(CcpJsonRepresentation message, CcpJsonRepresentation result) {

		String typedValueToRedirect = result.getAsString(JbBotEngineFields.typedValueToRedirect);
		String typedValue = message.getAsString(JnJsonInstantMessengerFields.message);
		boolean nothingToRedirect = typedValueToRedirect.isEmpty() || typedValueToRedirect.equals(typedValue);

		if(nothingToRedirect) {
			return result;
		}

		CcpJsonRepresentation messageWithoutTheFlag = message.removeFields(JbBotEngineFields.ticketFromTheList);
		CcpJsonRepresentation redirectedMessage = messageWithoutTheFlag.put(JnJsonInstantMessengerFields.message, typedValueToRedirect);
		boolean ticketFromTheList = result.getAsBoolean(JbBotEngineFields.ticketFromTheList);

		if(ticketFromTheList) {
			redirectedMessage = redirectedMessage.put(JbBotEngineFields.ticketFromTheList, true);
		}

		CcpJsonRepresentation redirectedResult = this.execute(redirectedMessage);
		return redirectedResult;
	}

	/**
	 * Returns the command named in the session.
	 * @param json the session
	 * @return the command, or {@code null} when unknown
	 */
	protected BotCommand getCommand(CcpJsonRepresentation json) {
		String commandName = json.getAsString(JnJsonInstantMessengerFields.commandName);
		BotCommand botCommand = JbBotEngine.INSTANCE.allCommands.get(commandName);
		return botCommand;
	} 
	
	/**
	 * Returns the name of the bot.
	 * @return the name of the bot type
	 */
	public String name() {
		String botTypeName3 = this.botType.name();
		return botTypeName3;
	}
	
	/**
	 * Tells whether the bot has an explanation in the language of the session.
	 * @param json the session
	 * @return {@code true} when there is one
	 */
	public boolean hasExplanation(CcpJsonRepresentation json) {
		String language = json.getAsString(JnJsonCommonsFields.language);
		Stream<CcpJsonRepresentation> stream4 = this.explanations.stream();
		var filter3 = stream4.filter(x -> x.getAsString(JnJsonCommonsFields.language).equals(language));
		var filter3Map = filter3
		.map(x -> x.getAsString(JnJsonCommonsFields.language));
		Optional<String> findFirst = filter3Map
		.findFirst();
		
		boolean response = findFirst.isPresent();
		return response;
	}

	/**
	 * Returns the explanation of the bot in the language of the session.
	 * @param json the session
	 * @return the explanation
	 * @throws JbErrorBotExplanationLanguageIsMissing when there is none in the language
	 */
	public String getExplanation(CcpJsonRepresentation json) {
		String language = json.getAsString(JnJsonCommonsFields.language);
		Stream<CcpJsonRepresentation> stream5 = this.explanations.stream();
		var filter4 = stream5
				.filter(x -> x.getAsString(JnJsonCommonsFields.language).equals(language));
				var filter5 = filter4
				.filter(x -> x.getAsString(JnJsonInstantMessengerFields.botName).equals(this.name()));
				var filter5Map = filter5
				.map(x -> x.getAsString(JnJsonInstantMessengerFields.message));
				Optional<String> findFirst = filter5Map
		.findFirst();
		
		String response = findFirst.orElseThrow(() -> new JbErrorBotExplanationLanguageIsMissing(language, this.name()));
		return response;
	}
	
	/**
	 * Returns the commands of the bot.
	 * @param json the session
	 * @return the commands
	 */
	List<JbBotBusiness> getAllCommands(CcpJsonRepresentation json){
		Stream<String> stream6 = this.commands.stream();
		var stream6Map = stream6.map(x -> this.getCommand(x));
		List<JbBotBusiness> collect = stream6Map.collect(Collectors.toList());
		return collect;
	}
	
	/**
	 * Returns a command by its name.
	 * @param commandName the name
	 * @return the command
	 * @throws JbErrorBotCommandNotFound when it is not registered
	 */
	protected JbBotBusiness getCommand(String commandName) {
		Collection<BotCommand> allCommandsValues = JbBotEngine.INSTANCE.allCommands.values();
		Stream<BotCommand> stream7 = allCommandsValues.stream();
		var filter6 = stream7.filter(x -> x.name.equals(commandName));
		Optional<BotCommand> findFirst = filter6.findFirst();
		boolean findFirstPresent2 = findFirst.isPresent();
		boolean commandNotFound = false == findFirstPresent2;
	
		if(commandNotFound) {
			JbErrorBotCommandNotFound jbErrorBotCommandNotFound = new JbErrorBotCommandNotFound(commandName);
			throw jbErrorBotCommandNotFound;
		}

		BotCommand botCommand = findFirst.get();
		return botCommand;
	}

	/** Raised when the bot has no explanation in the requested language. */
	@SuppressWarnings("serial")
	public static class JbErrorBotExplanationLanguageIsMissing extends RuntimeException {
		/**
		 * Names the missing language and the bot.
		 * @param language the language without an explanation
		 * @param botName the bot
		 */
		private JbErrorBotExplanationLanguageIsMissing(String language, String botName) {
			super("'" + language + "' is missing in the explanations of the bot '" + botName + "'");
		}
	}

	/** Raised when a command that is not registered is requested from the bot engine. */
	@SuppressWarnings("serial")
	public static class JbErrorBotCommandNotFound extends RuntimeException {
		/**
		 * Names the command not found.
		 * @param commandName the command
		 */
		private JbErrorBotCommandNotFound(String commandName) {
			super("The command '" + commandName + "' whas not found");
		}
	}
}
