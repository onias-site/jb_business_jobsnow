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
import com.jb.entities.JbEntityBotCommandStepEndMessage;
import com.jb.entities.JbEntityBotCommandStepExplanation;
import com.jb.entities.JbEntityBotCommandStepSession;
import com.jb.entities.JbEntityBotCommandStepStartMessage;
import com.jb.entities.subfields.JbNextStepFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * A step of a bot command, loaded from the database: it sends its start messages, runs its engine (a business), sends its
 * end messages and moves the session to the next step. A {@code CcpErrorFlowDisturb} thrown by the engine picks, by its
 * status, an item of the step flow (messages and next step); a validation error sends the explanation of the step.
 */
class BotCommandStep implements JbBotBusiness{

	/** The name of the step. */
	private final String name;
	/** The step that follows when the engine ends normally; blank ends the command. */
	private final String nextStep;
	/** The business run by the step. */
	private final CcpBusiness engine;
	/** The messages sent after the engine. */
	private final List<CcpJsonRepresentation> endMessages;
	/** The explanations of the step, sent on a validation error. */
	private final List<CcpJsonRepresentation> explanations;
	/** The flow items by status. */
	private final Map<Integer, CcpJsonRepresentation> flow;
	/** The messages sent before the engine. */
	private final List<CcpJsonRepresentation> startMessages;
	
	/**
	 * Loads the step with the engine named in the database.
	 * @param name the name of the step
	 * @param result the search result
	 */
	BotCommandStep(String name, CcpSelectUnionAll result) {
		this(name, loadEngine(name, result), result);
	}

	/**
	 * Instantiates the engine named in the step (no engine means doing nothing).
	 * @param name the name of the step
	 * @param result the search result
	 * @return the engine
	 */
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

	/**
	 * Loads the step with the given engine.
	 * @param name the name of the step
	 * @param engine the engine
	 * @param result the search result
	 */
	BotCommandStep(String name, CcpBusiness engine, CcpSelectUnionAll result) {

		this.name = name;
		this.engine = engine;
		this.flow = this.loadFlow(name, result);
		this.endMessages = result.getEntityRows(JbEntityBotCommandStepEndMessage.ENTITY);
		this.nextStep = loadFieldValue(name, result, JbEntityBotCommandStep.Fields.nextStep);
		this.startMessages = result.getEntityRows(JbEntityBotCommandStepStartMessage.ENTITY);
		this.explanations = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandStepExplanation.ENTITY, JnJsonInstantMessengerFields.stepName, JnJsonCommonsFields.language, JnJsonInstantMessengerFields.message);
	}

	/**
	 * Reads a field of the step record.
	 * @param name the name of the step
	 * @param result the search result
	 * @param field the field
	 * @return the value
	 */
	private static String loadFieldValue(String name, CcpSelectUnionAll result, CcpJsonFieldName field) {
		CcpJsonRepresentation entityRow = getEntityRow(name, result);
		String nextStep = entityRow.getAsString(field);
		return nextStep;
	}

	/**
	 * Saves the session at the next step, in the database and in memory.
	 * @param json the session
	 * @param nextStep the next step
	 * @return the saved session
	 */
	private CcpJsonRepresentation saveSession(CcpJsonRepresentation json, String nextStep) {
		
		CcpJsonRepresentation newJson = json.put(JnJsonInstantMessengerFields.stepName, nextStep);
		
		CcpJsonRepresentation savedSession = newJson.getTransformedJson(JsonProducers.sessionValuesProducer);

		JbEntityBotCommandStepSession.ENTITY.save(savedSession);
		BotCommand loadedCommand = this.getLoadedCommand(json);
		loadedCommand.putSession(savedSession);
		return savedSession;
	}

	/**
	 * Reads the flow items of the step by status.
	 * @param name the name of the step
	 * @param result the search result
	 * @return the flow items by status
	 */
	private Map<Integer, CcpJsonRepresentation> loadFlow(String name, CcpSelectUnionAll result) {
		Map<Integer, CcpJsonRepresentation> stepFlow = new HashMap<>();
		CcpJsonRepresentation entityRow = getEntityRow(name, result);
		List<CcpJsonRepresentation> allNextSteps = entityRow.getAsJsonList(JbEntityBotCommandStep.Fields.stepFlow);
		for (CcpJsonRepresentation json : allNextSteps) {
			Integer status = json.getAsIntegerNumber(JnJsonCommonsFields.status);
			stepFlow.put(status, json);
		}
		return stepFlow;
	}

	/**
	 * Reads the step record.
	 * @param name the name of the step
	 * @param result the search result
	 * @return the step record
	 */
	private static CcpJsonRepresentation getEntityRow(String name, CcpSelectUnionAll result) {
		CcpJsonRepresentation parametersToSearch = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.stepName, name);
		Supplier<CcpJsonRepresentation> jsonSupplier = parametersToSearch.getJsonSupplier();
		CcpJsonRepresentation entityRow = JbEntityBotCommandStep.ENTITY.getRecordFromUnionAll(result, jsonSupplier);
		return entityRow;
	}
	
	/**
	 * Returns the name of the step.
	 * @return the name
	 */
	public String toString() {
		return this.name;
	}

	/**
	 * Runs the step. When there is no next step (or the flow item has none), the session ends and the pending ticket of the
	 * command is closed ({@code PendingTicket.commandFinished}); an unforeseen status ends the session.
	 * @param json the session
	 * @return the result of the step
	 */
	@SuppressWarnings("unchecked")
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation ummutableFields = json.getJsonPiece(JnJsonInstantMessengerFields.botName, JnJsonInstantMessengerFields.chatId, JnJsonInstantMessengerFields.commandName);
		// what arrived at the step, before the engine adds anything: it tells which ticket the command solves
		CcpJsonRepresentation stepInput = json;
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

			boolean hasMoreSteps = conditionIfHasMoreSession.test(endMessagesResult);
			CcpJsonRepresentation result = endMessagesResult.getTransformedJsonConsideringIfAnyOfTheConditionsIsMet(updateSession, JbDefaultBotCommandStep.removeSession, conditionIfHasMoreSession);

			if(hasMoreSteps) {
				return result;
			}

			CcpJsonRepresentation commandFinished = PendingTicket.commandFinished(stepInput, result);
			return commandFinished;
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

			List<CcpJsonRepresentation> messages = flow.getAsJsonList(JbNextStepFields.message);
			// the flow message is resolved with what the engine had produced when it diverted the flow, so a
			// message can carry values calculated by the engine, and not only the ones that arrived at the step
			CcpJsonRepresentation jsonWithWhatTheEngineProduced = json.mergeWithAnotherJson(e.json);
			CcpJsonRepresentation copy = jsonWithWhatTheEngineProduced.copy();
			json = messages.stream().filter(x -> x.getAsString(JnJsonCommonsFields.language).equals(copy.getAsString(JnJsonCommonsFields.language)))
			.map(message -> bot.sendMessage(copy, message))
			.findFirst()
			.orElseGet(copy.getJsonSupplier())
			;
			
			String nextStepName = flow.getAsString(JbNextStepFields.nextStep);
			
			boolean hasNoNextStep = nextStepName.isEmpty();
		
			if(hasNoNextStep) {
				CcpJsonRepresentation execute = JbDefaultBotCommandStep.removeSession.execute(json);
				CcpJsonRepresentation commandFinished = PendingTicket.commandFinished(stepInput, execute);
				return commandFinished;
			}
			
			CcpJsonRepresentation jsonPreservingUmmatableFields = e.json.mergeWithAnotherJson(ummutableFields);
			CcpJsonRepresentation savedSession = this.saveSession(jsonPreservingUmmatableFields, nextStepName);
			return savedSession;
		}
	}

	
	/**
	 * Returns the name of the step.
	 * @return the name
	 */
	public String name() {
		return this.name;
	}
}
