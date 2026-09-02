package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpReflectionConstructorDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.jb.entities.JbEntityBotCommandStep;
import com.jb.entities.JbEntityBotCommandStepEndMessage;
import com.jb.entities.JbEntityBotCommandStepExplanation;
import com.jb.entities.JbEntityBotCommandStepFlowMessage;
import com.jb.entities.JbEntityBotCommandStepSession;
import com.jb.entities.JbEntityBotCommandStepStartMessage;
import java.util.stream.Stream;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;

class BotCommandStep implements JbBotBusiness{

	private final String name;
	private final String nextStep;
	private final CcpBusiness engine;
	private final Map<Integer, String> stepFlow;
	private final List<CcpJsonRepresentation> endMessages;
	private final List<CcpJsonRepresentation> explanations;
	private final List<CcpJsonRepresentation> startMessages;
	private final Map<Integer, List<CcpJsonRepresentation>> flowMessage;
	
	BotCommandStep(String name, CcpSelectUnionAll result) {
		this(name, loadEngine(name, result), result);
	}

	private static CcpBusiness loadEngine(String name, CcpSelectUnionAll result) {
		
		String engineName = loadFieldValue(name, result, JbEntityBotCommandStep.Fields.engine);
		String engineNameTrim = engineName.trim();

		boolean hasNoEngine = engineNameTrim.isEmpty();
		
		if(hasNoEngine) {
			return CcpOtherConstants.DO_NOTHING;
		}
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator(engineName);
		CcpReflectionConstructorDecorator reflection = ccpStringDecorator.reflection();
		CcpBusiness newInstance = reflection.newInstance();
		return newInstance;
	}

	BotCommandStep(String name, CcpBusiness engine, CcpSelectUnionAll result) {

		this.name = name;
		this.engine = engine;
		this.stepFlow = this.loadStepFlow(name, result);
		this.flowMessage = this.loadFlowMessage(name, result);
		this.endMessages = result.getEntityRows(JbEntityBotCommandStepEndMessage.ENTITY);
		this.nextStep = loadFieldValue(name, result, JbEntityBotCommandStep.Fields.nextStep);
		this.startMessages = result.getEntityRows(JbEntityBotCommandStepStartMessage.ENTITY);
		this.explanations = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandStepExplanation.ENTITY, JnJsonInstantMessengerFields.stepName, JnJsonCommonsFields.language, JnJsonInstantMessengerFields.message);
	}

	private static String loadFieldValue(String name, CcpSelectUnionAll result, CcpJsonFieldName field) {
		CcpJsonRepresentation entityRow = getEntityRow(name, result);
		String nextStep = entityRow.getAsString(field);
		return nextStep;
	}

	private CcpJsonRepresentation saveSession(CcpJsonRepresentation json, String nextStep) {
		
		CcpJsonRepresentation newJson = json.put(JnJsonInstantMessengerFields.stepName, nextStep);
		
		CcpJsonRepresentation savedSession = newJson.getTransformedJson(JsonProducers.sessionValuesProducer);
		
		JbEntityBotCommandStepSession.ENTITY.save(savedSession);
		BotCommand loadedCommand = this.getLoadedCommand(json);
		loadedCommand.putSession(savedSession);
		return savedSession;
	}

	private Map<Integer, String> loadStepFlow(String name, CcpSelectUnionAll result) {
		Map<Integer, String> stepFlow = new HashMap<>();
		CcpJsonRepresentation entityRow = getEntityRow(name, result);
		List<CcpJsonRepresentation> asJsonList = entityRow.getAsJsonList(JbEntityBotCommandStep.Fields.stepFlow);
		for (CcpJsonRepresentation json : asJsonList) {
			Integer status = json.getAsIntegerNumber(JnJsonCommonsFields.status);
			String stepName = json.getAsString(JnJsonInstantMessengerFields.stepName);
			stepFlow.put(status, stepName);
		}
		return stepFlow;
	}

	private static CcpJsonRepresentation getEntityRow(String name, CcpSelectUnionAll result) {
		CcpJsonRepresentation parametersToSearch = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.stepName, name);
		Supplier<CcpJsonRepresentation> jsonSupplier = parametersToSearch.getJsonSupplier();
		CcpJsonRepresentation entityRow = JbEntityBotCommandStep.ENTITY.getRecordFromUnionAll(result, jsonSupplier);
		return entityRow;
	}
	
	private Map<Integer, List<CcpJsonRepresentation>> loadFlowMessage(String stepName, CcpSelectUnionAll resultFromSearchAllSteps) {
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JbEntityBotCommandStepFlowMessage.ENTITY);
		Stream<CcpJsonRepresentation> stream = entityRows.stream();
		var filter = stream.filter(x -> x.getAsString(JnJsonInstantMessengerFields.stepName).equals(stepName));
		List<CcpJsonRepresentation> collect = filter.collect(Collectors.toList());
		var stream2 = new ArrayList<>(collect).stream();
		var stream2Map = stream2.map(x -> x.getAsIntegerNumber(JnJsonCommonsFields.status));
		Set<Integer> allStatus = stream2Map
		.collect(Collectors.toSet());

		var response = new HashMap<Integer, List<CcpJsonRepresentation>>();
		
		for (Integer status : allStatus) {
			var stream3 = new ArrayList<>(collect).stream();
			var filter2 = stream3.filter(x -> x.getAsIntegerNumber(JnJsonCommonsFields.status).equals(status));
			List<CcpJsonRepresentation> filtered = filter2.collect(Collectors.toList());
			response.put(status, filtered);
		}
		
		return response;
	}

	public String toString() {
		return this.name;
	}

	@SuppressWarnings("unchecked")
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation ummutableFields = json.getJsonPiece(JnJsonInstantMessengerFields.botName, JnJsonInstantMessengerFields.chatId, JnJsonInstantMessengerFields.commandName);
		Bot bot = this.getBot(json);
		try {
			CcpJsonRepresentation sendMessageResult = bot.sendMessage(json, this.startMessages);
			boolean hasStartMessage = sendMessageResult.containsAllFields(JbBotEngine.Fields.replyTo);
		
			if(hasStartMessage) {
				Long replyTo = sendMessageResult.getAsLongNumber(JbBotEngine.Fields.replyTo);
				json = json.put(JbBotEngine.Fields.replyTo, replyTo);
			}
			
			CcpJsonRepresentation engineResult = this.engine.execute(json);
			Predicate<CcpJsonRepresentation> conditionIfHasMoreSession = jsn -> false == this.nextStep.trim().isEmpty() && JbBotEngine.INSTANCE.allSteps.containsKey(this.nextStep);
			
			CcpBusiness updateSession = jsn -> {
				CcpJsonRepresentation jsonPreservingUmmatableFields = jsn.mergeWithAnotherJson(ummutableFields);
				CcpJsonRepresentation savedSession = this.saveSession(jsonPreservingUmmatableFields, this.nextStep);
				return savedSession;
			};

			CcpJsonRepresentation endMessagesResult = bot.sendMessage(engineResult, this.endMessages);

			CcpJsonRepresentation result = endMessagesResult.getTransformedJsonConsideringIfAnyOfTheConditionsIsMet(updateSession, JbDefaultBotCommandStep.removeSession, conditionIfHasMoreSession);
			
			return result;
		} catch(CcpJsonValidationError e) {
			json = bot.sendMessage(json, this.explanations);
			return json;
		} catch (CcpErrorFlowDisturb e) {
			int asNumber2 = e.status.asNumber();
		
			List<CcpJsonRepresentation> messages = this.flowMessage.get(asNumber2);

			json = bot.sendMessage(json, messages);
			
			Predicate<CcpJsonRepresentation> conditionIfHasMoreSession = jsn -> this.stepFlow.containsKey(e.status.asNumber());
			
			CcpBusiness updateSession = ex -> {
				int asNumber = e.status.asNumber();
				String nextStep = this.stepFlow.get(asNumber);
				CcpJsonRepresentation jsonPreservingUmmatableFields = e.json.mergeWithAnotherJson(ummutableFields);
				CcpJsonRepresentation savedSession = this.saveSession(jsonPreservingUmmatableFields, nextStep);
				return savedSession;
			};
			CcpBusiness removeSession = ex -> JbDefaultBotCommandStep.removeSession.execute(e.json);
			
			CcpJsonRepresentation result = e.json.getTransformedJsonConsideringIfAnyOfTheConditionsIsMet(updateSession, removeSession, conditionIfHasMoreSession);
			
			return result;
		}
	}
	
	public String name() {
		return this.name;
	}
}
