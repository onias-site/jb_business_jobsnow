package com.jb.business.bots.engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jb.entities.JbEntityBotCommand;
import com.jb.entities.JbEntityBotCommandExplanation;
import com.jb.entities.JbEntityBotCommandName;
import com.jn.entities.JnEntitySupportPendingCommand;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;


/**
 * A bot command, loaded from the database: its names and explanations by language and the names of its parameters. It
 * also keeps the sessions of the chats in memory.
 */
class BotCommand implements JbBotBusiness{ 
	
	/** The canonical name of the command. */
	final String name;	
	/** The names of the parameters, in the order they are typed. */
	private final List<String> parameterNames;
	/** The names of the command, by language. */
	private final List<CcpJsonRepresentation> names;
	/** The explanations of the command, by language. */
	private final List<CcpJsonRepresentation> explanations;
	/** The sessions in memory, by chat id. */
	private final Map<Long, CcpJsonRepresentation> sessions = new HashMap<>();
	
	/**
	 * Loads the command from the search result.
	 * @param name the canonical name
	 * @param result the search result
	 */
	BotCommand(String name, CcpSelectUnionAll result) {

		this.explanations = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandExplanation.ENTITY, JnJsonInstantMessengerFields.commandName, JnJsonCommonsFields.language, JnJsonInstantMessengerFields.message);
		this.names = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandName.ENTITY, JnJsonInstantMessengerFields.commandName, JnJsonCommonsFields.language, JnJsonInstantMessengerFields.message);
		this.parameterNames = this.loadParameterNames(name, result);
		this.name = name;
	}

	/**
	 * Puts the command as the command and the step of the session, dropping the saved session JSON.
	 * @param json the session
	 * @return the session at the start of the command
	 */
	public CcpJsonRepresentation getCommandJson(CcpJsonRepresentation json) {
		CcpJsonRepresentation putSameValueInManyFields = json
				.putSameValueInManyFields(this.name, JnJsonInstantMessengerFields.commandName, JnJsonInstantMessengerFields.stepName);
				CcpJsonRepresentation put = putSameValueInManyFields
				.removeFields(JnJsonCommonsFields.json)
				;
		return put;
	}

	/**
	 * Tells whether the first word typed is not this command (by its name in the language of the session).
	 * @param json the session
	 * @return {@code true} when the user did not type this command
	 */
	public boolean commandNameDoesNotMatch(CcpJsonRepresentation json) {
		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String[] split = typedValue.split(" ");
		List<String> asList = Arrays.asList(split);
		String first = asList.get(0);
		String identifier = this.getIdentifier(json);
		String firstTrim = first.trim();
		String identifierTrim = identifier.trim();
		boolean firstTrimEquals = firstTrim.equals(identifierTrim);

		boolean commandNameDoesNotMatch = false == firstTrimEquals;
		return commandNameDoesNotMatch;
	}

	/**
	 * Reads the parameter names of the command.
	 * @param name the canonical name
	 * @param result the search result
	 * @return the parameter names
	 */
	List<String> loadParameterNames(String name, CcpSelectUnionAll result) {
		Supplier<CcpJsonRepresentation> jsonSupplier = () -> 
		CcpOtherConstants.EMPTY_JSON
		.put(JnJsonInstantMessengerFields.commandName, name)
		;
		CcpJsonRepresentation recordFromUnionAll = JbEntityBotCommand.ENTITY.getRecordFromUnionAll(result, jsonSupplier);
		List<String> parameterNames = recordFromUnionAll.getAsStringList(JbEntityBotCommand.Fields.parameterName);
		return parameterNames;
	}

	/**
	 * Returns the canonical name.
	 * @return the name
	 */
	public String toString() {
		return this.name;
	}

	/**
	 * Puts the parameters and runs the current step of the session.
	 * @param json the session
	 * @return the result of the step
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation putParameters = this.putParameters(json);

		String stepName = putParameters.getAsString(JnJsonInstantMessengerFields.stepName);
		
		JbBotBusiness step = JbBotEngine.INSTANCE.allSteps.get(stepName);
		
		CcpJsonRepresentation apply = step.execute(putParameters);

		return apply;
	}
	
	/**
	 * When the command itself was typed, puts the pending ticket and the typed parameters (split by any whitespace) under
	 * their names; the answer to a later step is left alone.
	 * @param json the session
	 * @return the session with the parameters
	 */
	private CcpJsonRepresentation putParameters(CcpJsonRepresentation json) {

		// the parameters come only with the command itself; in the later steps of the session the typed text is
		// the answer to the step, and reading its words as parameters would overwrite the ones given with the command
		boolean isAnswerToAStep = this.commandNameDoesNotMatch(json);

		if(isAnswerToAStep) {
			return json;
		}

		// the operator types the command by hand: two spaces between the words would make an empty parameter and
		// shift the following ones
		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String trimmedTypedValue = typedValue.trim();
		String[] split = trimmedTypedValue.split("\\s+");
		List<String> asList = Arrays.asList(split);
		int size = asList.size();
		List<String> parameterValues = asList.subList(1, size);
		json = this.putPendingTicket(json, parameterValues);
		int k = 0;

		for (String parameterName : this.parameterNames) {
			int size2 = parameterValues.size();
			boolean noMoreParameterValues = k >= size2;
			if(noMoreParameterValues) {
				break;
			}
			String parameterValue = parameterValues.get(k++);
			CcpFieldName ccpFieldName = new CcpFieldName(parameterName);
			json = json.put(ccpFieldName, parameterValue);
		}
		
		;
		
		return json;
	}

	/**
	 * The ticket this command solves, in the same form the message sending recorded it when the command was sent
	 * to the operator: the canonical name of the command (and not the name the operator typed, which may be the
	 * one of their language) followed by the parameters.
	 */
	private CcpJsonRepresentation putPendingTicket(CcpJsonRepresentation json, List<String> parameterValues) {
		String parameters = String.join(" ", parameterValues);
		String command = "/" + this.name + " " + parameters;
		String pendingTicket = JnEntitySupportPendingCommand.normalize(command);
		CcpJsonRepresentation jsonWithPendingTicket = json.put(JbBotEngineFields.pendingTicket, pendingTicket);
		return jsonWithPendingTicket;
	}

	/**
	 * Tells whether the command has an explanation in the language of the session.
	 * @param json the session
	 * @return {@code true} when there is one
	 */
	public boolean hasExplanation(CcpJsonRepresentation json) {
		String language = json.getAsString(JnJsonCommonsFields.language);
		Stream<CcpJsonRepresentation> stream = this.explanations.stream();
		var filter = stream.filter(x -> x.getAsString(JnJsonCommonsFields.language).equals(language));
		var filterMap = filter
		.map(x -> x.getAsString(JnJsonCommonsFields.language));
		Optional<String> findFirst = filterMap
		.findFirst();
		
		boolean response = findFirst.isPresent();
		return response;
	}

	/**
	 * Returns the explanation in the language of the session (see finding: it returns the language, not the text).
	 * @param json the session
	 * @return the explanation, or an empty text
	 */
	public String getExplanation(CcpJsonRepresentation json) {
		String language = json.getAsString(JnJsonCommonsFields.language);
		Stream<CcpJsonRepresentation> stream2 = this.explanations.stream();
		var filter2 = stream2.filter(x -> x.getAsString(JnJsonCommonsFields.language).equals(language));
		var filter2Map = filter2
		.map(x -> x.getAsString(JnJsonCommonsFields.language));
		Optional<String> findFirst = filter2Map
		.findFirst();
		
		String response = findFirst.orElse("");
		return response;
	} 

	/**
	 * Returns how the command is typed in the language of the session: {@code /} plus its name.
	 * @param json the session
	 * @return the identifier
	 */
	public String getIdentifier(CcpJsonRepresentation json) {
		
		String language = json.getAsString(JnJsonCommonsFields.language);
		Stream<CcpJsonRepresentation> stream3 = this.names.stream();
		var filter3 = stream3.filter(x -> x.getAsString(JnJsonCommonsFields.language).equals(language));
		var filter3Map = filter3
		.map(x -> "/" + x.getAsString(JnJsonInstantMessengerFields.commandName));
		var findFirst2 = filter3Map
		.findFirst();

		String response = findFirst2.orElseGet(() -> "/" + this.name);
		
		return response;
	}

	/**
	 * Tells whether the command is visible to the user: a default command decides by itself, the others by their first
	 * step.
	 * @param json the session
	 * @return {@code true} when visible
	 */
	public boolean isVisible(CcpJsonRepresentation json) {
		try {
			JbDefaultBotCommandStep valueOf = JbDefaultBotCommandStep.valueOf(this.name);
			boolean visible = valueOf.isVisible(json);
			return visible;
			
		} catch (Exception e) {
			JbBotBusiness firstStep = JbBotEngine.INSTANCE.allSteps.get(this.name);
			
			boolean listed = firstStep.isVisible(json);
			
			return listed;
		}
	}
	
	/**
	 * Returns the canonical name.
	 * @return the name
	 */
	public String name() {
		return this.name;
	}
	
	/**
	 * Keeps the session of the chat in memory.
	 * @param json the session
	 */
	void putSession(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		this.sessions.put(chatId, json);
	}
	
	/**
	 * Removes the session of the chat from memory, only when it is the given one (see finding).
	 * @param json the session
	 */
	void removeSession(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		this.sessions.remove(chatId, json);
	}

	/**
	 * Returns the session of the chat kept in memory.
	 * @param json the message
	 * @return the session, or {@code null}
	 */
	CcpJsonRepresentation getSession(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		CcpJsonRepresentation session = this.sessions.get(chatId);
		return session;
	}

	/**
	 * Tells whether the chat has a session in memory.
	 * @param json the message
	 * @return {@code true} when it has
	 */
	boolean hasSession(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		boolean session = this.sessions.containsKey(chatId);
		return session;
	}
}
