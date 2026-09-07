package com.jb.business.bots.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpReflectionConstructorDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.jb.entities.JbEntityBotCommandStep;
import com.jb.entities.JbEntityBotCommandStep.JbNextStepFields;
import com.jb.entities.JbEntityBotCommandStepEndMessage;
import com.jb.entities.JbEntityBotCommandStepExplanation;
import com.jb.entities.JbEntityBotCommandStepSession;
import com.jb.entities.JbEntityBotCommandStepStartMessage;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

class BotCommandStep implements JbBotBusiness{

	private final String name;
	private final String nextStep;
	private final CcpBusiness engine;
	private final List<CcpJsonRepresentation> endMessages;
	private final List<CcpJsonRepresentation> explanations;
	private final Map<Integer, CcpJsonRepresentation> flow;
	private final List<CcpJsonRepresentation> startMessages;
	
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
		this.flow = this.loadFlow(name, result);
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

	private Map<Integer, CcpJsonRepresentation> loadFlow(String name, CcpSelectUnionAll result) {
		Map<Integer, CcpJsonRepresentation> stepFlow = new HashMap<>();
		CcpJsonRepresentation entityRow = getEntityRow(name, result);
		List<CcpJsonRepresentation> allNextSteps = entityRow.getAsJsonList(JbEntityBotCommandStep.Fields.stepFlow);
		for (CcpJsonRepresentation json : allNextSteps) {
			Integer status = json.getAsIntegerNumber(JnJsonCommonsFields.status);
			CcpJsonRepresentation nextStep = json.getInnerJson(JnJsonInstantMessengerFields.stepName);
			stepFlow.put(status, nextStep);
		}
		return stepFlow;
	}

	private static CcpJsonRepresentation getEntityRow(String name, CcpSelectUnionAll result) {
		CcpJsonRepresentation parametersToSearch = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.stepName, name);
		Supplier<CcpJsonRepresentation> jsonSupplier = parametersToSearch.getJsonSupplier();
		CcpJsonRepresentation entityRow = JbEntityBotCommandStep.ENTITY.getRecordFromUnionAll(result, jsonSupplier);
		return entityRow;
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
			
			boolean hasExplanations = false == this.explanations.isEmpty();

			if(hasExplanations) {
				CcpJsonRepresentation sendMessage = bot.sendMessage(json, this.explanations);
				return sendMessage;
			}

			String message = e.getExplanedMessage();
			json = bot.sendMessage(json, message);
			return json;
		} catch (CcpErrorFlowDisturb e) {
			int status = e.status.asNumber();
		
			CcpJsonRepresentation flow = this.flow.getOrDefault(status, CcpOtherConstants.EMPTY_JSON);
			
			boolean unforeseenStatus = flow.isEmpty();
			
			if(unforeseenStatus) {
				CcpJsonRepresentation execute = JbDefaultBotCommandStep.removeSession.execute(json);
				return execute;
			}

			List<CcpJsonRepresentation> message = flow.getAsJsonList(JbNextStepFields.message);
			
			json = bot.sendMessage(json, message);
			
			String nextStepName = flow.getAsString(JbNextStepFields.nextStep);
			
			boolean hasNoNextStep = nextStepName.isEmpty();
		
			if(hasNoNextStep) {
				CcpJsonRepresentation execute = JbDefaultBotCommandStep.removeSession.execute(json);
				return execute;
			}
			
			CcpJsonRepresentation jsonPreservingUmmatableFields = e.json.mergeWithAnotherJson(ummutableFields);
			CcpJsonRepresentation savedSession = this.saveSession(jsonPreservingUmmatableFields, nextStepName);
			return savedSession;
		}
	}

	
	public String name() {
		return this.name;
	}
}
