package com.jb.business.bots.engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jb.business.bots.login.token.JbBotSolveLoginTokenTicket.StepFields;
import com.jb.entities.JbEntityBotCommand;
import com.jb.entities.JbEntityBotCommandExplanation;
import com.jb.entities.JbEntityBotCommandName;
import java.util.stream.Stream;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;

class BotCommand implements JbBotBusiness{
	
	final String name;	
	private final List<String> parameterNames;
	private final List<CcpJsonRepresentation> names;
	private final List<CcpJsonRepresentation> explanations;
	private final Map<Long, CcpJsonRepresentation> sessions = new HashMap<>();
	
	BotCommand(String name, CcpSelectUnionAll result) {

		this.explanations = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandExplanation.ENTITY, JnJsonInstantMessengerFields.commandName, JnJsonCommonsFields.language, JnJsonInstantMessengerFields.message);
		this.names = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandName.ENTITY, JnJsonInstantMessengerFields.commandName, JnJsonCommonsFields.language, JnJsonInstantMessengerFields.message);
		this.parameterNames = this.loadParameterNames(name, result);
		this.name = name;
	}

	public CcpJsonRepresentation getCommandJson(CcpJsonRepresentation json) {
		CcpJsonRepresentation putSameValueInManyFields = json
				.putSameValueInManyFields(this.name, JnJsonInstantMessengerFields.commandName, JnJsonInstantMessengerFields.stepName);
				CcpJsonRepresentation put = putSameValueInManyFields
				.removeFields(JnJsonCommonsFields.json)
				;
		return put;
	}

	public boolean commandNameDoesNotMatch(CcpJsonRepresentation json) {
		String typedValue = json.getAsString(StepFields.typedValue);
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

	List<String> loadParameterNames(String name, CcpSelectUnionAll result) {
		Supplier<CcpJsonRepresentation> jsonSupplier = () -> 
		CcpOtherConstants.EMPTY_JSON
		.put(JnJsonInstantMessengerFields.commandName, name)
		;
		CcpJsonRepresentation recordFromUnionAll = JbEntityBotCommand.ENTITY.getRecordFromUnionAll(result, jsonSupplier);
		List<String> parameterNames = recordFromUnionAll.getAsStringList(JbEntityBotCommand.Fields.parameterName);
		return parameterNames;
	}

	public String toString() {
		return this.name;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation putParameters = this.putParameters(json);

		String stepName = putParameters.getAsString(JnJsonInstantMessengerFields.stepName);
		
		JbBotBusiness step = JbBotEngine.INSTANCE.allSteps.get(stepName);
		
		CcpJsonRepresentation apply = step.execute(putParameters);

		return apply;
	}
	
	private CcpJsonRepresentation putParameters(CcpJsonRepresentation json) {
		String typedValue = json.getAsString(StepFields.typedValue);
		String[] split = typedValue.split(" ");
		List<String> asList = Arrays.asList(split);
		int size = asList.size();
		List<String> parameterValues = asList.subList(1, size);
		int k = 0;
		
		for (String parameterName : this.parameterNames) {
			int size2 = parameterValues.size();
			boolean kMaiorOuIgual = k >= size2;
			if(kMaiorOuIgual) {
				break;
			}
			String parameterValue = parameterValues.get(k++);
			CcpFieldName ccpFieldName = new CcpFieldName(parameterName);
			json = json.put(ccpFieldName, parameterValue);
		}
		
		;
		
		return json;
	}

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
	
	public String name() {
		return this.name;
	}
	
	void putSession(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		this.sessions.put(chatId, json);
	}
	
	void removeSession(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		this.sessions.remove(chatId, json);
	}

	CcpJsonRepresentation getSession(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		CcpJsonRepresentation session = this.sessions.get(chatId);
		return session;
	}

	boolean hasSession(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		boolean session = this.sessions.containsKey(chatId);
		return session;
	}
}
