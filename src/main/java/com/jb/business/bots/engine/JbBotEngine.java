package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.instant.messenger.CcpInstantMessenger;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jb.entities.JbEntityBot;
import com.jb.entities.JbEntityBotCommand;
import com.jb.entities.JbEntityBotCommandExplanation;
import com.jb.entities.JbEntityBotCommandName;
import com.jb.entities.JbEntityBotCommandStep;
import com.jb.entities.JbEntityBotCommandStepEndMessage;
import com.jb.entities.JbEntityBotCommandStepExplanation;
import com.jb.entities.JbEntityBotCommandStepSession;
import com.jb.entities.JbEntityBotCommandStepStartMessage;
import com.jb.entities.JbEntityBotExplanation;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnSystemProperties;

public class JbBotEngine {
	
	public static final JbBotEngine INSTANCE = new JbBotEngine();
	
	public static enum Fields implements CcpJsonFieldName{
		bots, token, botType, replyTo, language, stepName, status
	}
	public static class Bot{
		public final BotType type;
		public final Set<BotCommand> commands;
		public final Map<String, String> explanations;

		public Bot(BotType type, List<BotCommand> botCommands, CcpSelectUnionAll resultFromSearchBots) {
			this.explanations = null;//FIXME
			this.commands = null;//FIXME
			this.type = type;
		}
		
		public String toString() {
			return this.type.name();
		}
	}
	
	public static class BotCommand implements CcpBusiness{
		
		public final String name;	
		public final Map<String, String> names;
		public final Map<String, String> explanations;
		
		public BotCommand(String name, CcpSelectUnionAll result) {

			this.explanations = this.getExplanations(name, result);
			this.names = this.getNames(name, result);
			this.name = name;
		
		}

		private Map<String, String> getExplanations(String name, CcpSelectUnionAll result) {
			List<CcpJsonRepresentation> entityRows = result.getEntityRows(JbEntityBotCommandExplanation.ENTITY);
			List<CcpJsonRepresentation> collect = entityRows.stream().filter(x -> x.getAsString(JbEntityBotCommandExplanation.Fields.commandName).equals(name)).collect(Collectors.toList());
			Map<String, String> response = new HashMap<>();
			for (CcpJsonRepresentation json : collect) {
				String language = json.getAsString(JbEntityBotCommandExplanation.Fields.language);
				String message = json.getAsString(JbEntityBotCommandExplanation.Fields.message);
				response.put(language, message);
			}
			return response;
		}
		
		private Map<String, String> getNames(String name, CcpSelectUnionAll result) {
			List<CcpJsonRepresentation> entityRows = result.getEntityRows(JbEntityBotCommandName.ENTITY);
			List<CcpJsonRepresentation> collect = entityRows.stream().filter(x -> x.getAsString(JbEntityBotCommandName.Fields.commandName).equals(name)).collect(Collectors.toList());
			Map<String, String> response = new HashMap<>();
			for (CcpJsonRepresentation json : collect) {
				String language = json.getAsString(JbEntityBotCommandName.Fields.language);
				String message = json.getAsString(JbEntityBotCommandName.Fields.message);
				response.put(language, message);
			}
			return response;
		}

		public boolean equals(Object obj) {
			try {
				boolean equals = ((BotCommand)obj).name.equals(this.name);
				return equals;
			} catch (Exception e) {
				return false;
			}
		}
		
		public int hashCode() {
			int hashCode = this.name.hashCode();
			return hashCode;
		}
		
		public String toString() {
			return this.name;
		}

		private String getStepName(CcpJsonRepresentation json) {
			{
				String stepName = json.getAsString(Fields.stepName);
				
				boolean hasCurrentStep = false == stepName.trim().isEmpty();
				
				if(hasCurrentStep) {
					return stepName;
				}	
			}
			
			CcpJsonRepresentation oneById = JbEntityBotCommandStepSession.ENTITY.getEntityDetails().getOneByIdOrHandleItIfThisIdWasNotFound(json, CcpOtherConstants.RETURNS_EMPTY_JSON);
			String stepName = oneById.getAsString(JbEntityBotCommandStepSession.Fields.stepName);
			
			boolean hasCurrentStep = false == stepName.trim().isEmpty();
			
			if(hasCurrentStep) {
				return stepName;
			}
			return this.name;
		} 	
		
		
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

			String stepName = this.getStepName(json);
			
			BotCommandStep botCommandStep = JbBotEngine.INSTANCE.allSteps.get(stepName);
			
			CcpJsonRepresentation apply = botCommandStep.apply(json);

			return apply;
		}
	}
	public static class BotCommandStep implements CcpBusiness{

		public final String name;
		public final CcpBusiness engine;
		public final Map<Integer, String> nextSteps;
		public final Map<String, String> explanations;
		public final Map<String, String> startMessages;
		public final Map<Integer, Map<String, String>> endMessages;
		
		public BotCommandStep(String name, String engine, Map<Integer, String> nextSteps, Map<String, String> startMessages, Map<String, String> explanations, Map<Integer, Map<String, String>> endMessages) {

			this.name = name;
			this.nextSteps = nextSteps;
			this.endMessages = endMessages;
			this.explanations = explanations;
			this.startMessages = startMessages;
			this.engine = new CcpStringDecorator(engine).reflection().newInstance();
		}

		public boolean equals(Object obj) {
			try {
				boolean equals = ((BotCommand)obj).name.equals(this.name);
				return equals;
			} catch (Exception e) {
				return false;
			}
		}
		
		public int hashCode() {
			int hashCode = this.name.hashCode();
			return hashCode;
		}
		
		public String toString() {
			return this.name;
		}

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			
			try {
				json = this.sendMessage(json, this.startMessages);
				CcpJsonRepresentation apply = this.engine.apply(json);
				JbEntityBotCommandStepSession.ENTITY.delete(json);
				return apply;
			} catch (CcpErrorFlowDisturb e) {
				
				boolean wrongValue = CcpProcessStatusDefault.UNPROCESSABLE_ENTITY.equals(e.status);
				
				if(wrongValue) {
					json = this.sendMessage(e.json, this.explanations);
					return json;
				}
				
				int status = e.status.asNumber();
				boolean hasEndMessage = this.endMessages.containsKey(status);
				
				if(hasEndMessage) {
					Map<String, String> messages = this.endMessages.get(status);
					json = this.sendMessage(e.json, messages);
				}
				
				
				boolean stepNotForeseen = false == this.nextSteps.containsKey(status);
				
				if(stepNotForeseen) {
					JbEntityBotCommandStepSession.ENTITY.delete(json);
					return e.json;
				}

				String nextStep = this.nextSteps.get(status);
				CcpJsonRepresentation newJson = e.json.put(JbEntityBotCommandStepSession.Fields.stepName, nextStep);
				JbEntityBotCommandStepSession.ENTITY.save(newJson);
				return newJson;
			
			}
		}

		private CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, Map<String, String> map) {
			
			String language = json.getAsString(Fields.language);
			boolean hasNotMessage = false == map.containsKey(language);

			if(hasNotMessage) {
				return json;
			}
			
			JnSystemProperties systemProperties = new JnSystemProperties();
			CcpInstantMessenger instantMessenger = CcpDependencyInjection.getDependency(CcpInstantMessenger.class);
			String botType = json.getAsString(Fields.botType);
			BotType botTypeValue = BotType.valueOf(botType);
			String botToken = systemProperties.getSystemInnerProperty(Fields.bots, botTypeValue, Fields.token) ;
			Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);
			Long replyTo = json.getAsLongNumber(Fields.replyTo);
			
			String message = map.get(language);
			CcpJsonRepresentation sendTextMessage = instantMessenger.sendTextMessage(botToken, chatId, replyTo, message);
			CcpJsonRepresentation mergeWithAnotherJson = json.mergeWithAnotherJson(sendTextMessage);
			return mergeWithAnotherJson;
		}

	}
	
	public static enum BotType implements CcpJsonFieldName{
		user, 
		support
		;
		
		protected CcpJsonRepresentation getParameterToSearchBot() {
			String botName =  this.name();
			var parameterToSearchBots = CcpOtherConstants.EMPTY_JSON
					.put(JbEntityBot.Fields.botName, botName)
					
			;
			return parameterToSearchBots;
		}
		
	}
	private final Map<String, BotCommandStep> allSteps;
	
	private JbBotEngine() {
		JnSystemProperties systemProperties = new JnSystemProperties();
		BotType[] bots = BotType.values();
		CcpJsonRepresentation[] parametersToSearchBots = new CcpJsonRepresentation[bots.length];
		List<String> languages = systemProperties.languages();
		
		int k = 0;
		for (BotType bot : bots) {
			for (var language : languages) {
				CcpJsonRepresentation parameterToSearchBots = bot.getParameterToSearchBot();
				parametersToSearchBots[k++] = parameterToSearchBots
						.put(JbEntityBotExplanation.Fields.language, language)
						;
				
			}
		}
		
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll resultFromSearchBots = crud.unionAll(parametersToSearchBots, JnDeleteKeysFromCache.INSTANCE, JbEntityBot.ENTITY, JbEntityBotExplanation.ENTITY);
		k = 0;
		
		CcpJsonRepresentation[] parametersToSearchCommandsAndFirstSteps = new CcpJsonRepresentation[bots.length];
		
		for (CcpJsonRepresentation parameterToSearchBot : parametersToSearchBots) {
			CcpJsonRepresentation recordFromUnionAll = JbEntityBot.ENTITY.getRecordFromUnionAll(resultFromSearchBots, parameterToSearchBot);
			List<String> commands = recordFromUnionAll.getAsStringList(JbEntityBot.Fields.commandName);
			for (String command : commands) {
				CcpJsonRepresentation parameterToSearchCommandsAndFirstSteps = recordFromUnionAll
						.put(JbEntityBotCommandStep.Fields.stepName, command)
						.put(JbEntityBotCommand.Fields.commandName, command);
				parametersToSearchCommandsAndFirstSteps[k++] = parameterToSearchCommandsAndFirstSteps;
			}
		}

		CcpSelectUnionAll resultFromSearchCommandsAndFirstSteps = crud.unionAll(parametersToSearchCommandsAndFirstSteps, JnDeleteKeysFromCache.INSTANCE, 
				JbEntityBotCommandStepExplanation.ENTITY, 
				JbEntityBotCommandExplanation.ENTITY, 
				JbEntityBotCommandStep.ENTITY, 
				JbEntityBotCommandName.ENTITY, 
				JbEntityBotCommand.ENTITY 
				);
		
		List<CcpJsonRepresentation> allSteps = new ArrayList<>();
		List<CcpJsonRepresentation> firstSteps = resultFromSearchCommandsAndFirstSteps.getEntityRows(JbEntityBotCommandStep.ENTITY);
		List<BotCommand> botCommands = new ArrayList<JbBotEngine.BotCommand>();
		
		for (CcpJsonRepresentation firstStep : firstSteps) {
			var nextSteps = firstStep.getAsJsonList(JbEntityBotCommandStep.Fields.nextStep);
			for (CcpJsonRepresentation nextStep : nextSteps) {
				for (var language : languages) {
					CcpJsonRepresentation put = nextStep
							.duplicateValueFromField(JbEntityBotCommandStep.Fields.stepName, JbEntityBotCommand.Fields.commandName)
							.put(JbEntityBotExplanation.Fields.language, language)
							;
					allSteps.add(put);
				}
			}
			String commandName = firstStep.getAsString(JbEntityBotCommand.Fields.commandName);
			BotCommand botCommand = new BotCommand(commandName, resultFromSearchCommandsAndFirstSteps);
			botCommands.add(botCommand);
		}
		
		
		
		CcpJsonRepresentation[] parametersToSearchAllSteps = allSteps.toArray(new CcpJsonRepresentation[allSteps.size()]);
		
		CcpSelectUnionAll resultFromSearchAllSteps = crud.unionAll(parametersToSearchAllSteps, JnDeleteKeysFromCache.INSTANCE, 
				JbEntityBotCommandStepStartMessage.ENTITY, 
				JbEntityBotCommandStepEndMessage.ENTITY, 
				JbEntityBotCommandStep.ENTITY 
				);
		
		Map<String, BotCommandStep> stepsMap = new HashMap<String, BotCommandStep>();
		
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JbEntityBotCommandStep.ENTITY);
		
		for (CcpJsonRepresentation entityRow : entityRows) {
			String stepName = entityRow.getAsString(JbEntityBotCommandStep.Fields.stepName);
			String engine = entityRow.getAsString(JbEntityBotCommandStep.Fields.engine);
			Map<Integer, String> nextSteps = this.getNextSteps(entityRow);
			Map<String, String> startMessages = this.getStartMessages(stepName, resultFromSearchAllSteps);
			Map<String, String> explanations = this.getExplanations(stepName, resultFromSearchAllSteps);
			Map<Integer, Map<String, String>> endMessages = this.getEndMessages(stepName, resultFromSearchAllSteps);
			BotCommandStep step = new BotCommandStep(stepName, engine, nextSteps, startMessages, explanations, endMessages);
			stepsMap.put(stepName, step);
		}
		
		this.allSteps = stepsMap;
	}

	private Map<Integer, Map<String, String>> getEndMessages(String stepName, CcpSelectUnionAll resultFromSearchAllSteps) {
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JbEntityBotCommandStepEndMessage.ENTITY);
		List<CcpJsonRepresentation> collect = entityRows.stream().filter(x -> x.getAsString(JbEntityBotCommandStepEndMessage.Fields.stepName).equals(stepName)).collect(Collectors.toList());
		Set<Integer> allStatus = new ArrayList<>(collect).stream().map(x -> x.getAsIntegerNumber(JbEntityBotCommandStepEndMessage.Fields.status))
		.collect(Collectors.toSet());

		var response = new HashMap<Integer, Map<String, String>>();
		for (Integer status : allStatus) {
			List<CcpJsonRepresentation> filtered = new ArrayList<>(collect).stream().filter(x -> x.getAsIntegerNumber(JbEntityBotCommandStepEndMessage.Fields.status).equals(status)).collect(Collectors.toList());
			Map<String, String> endMessages = new HashMap<String, String>();
			for (CcpJsonRepresentation json : filtered) {
				String language = json.getAsString(JbEntityBotCommandStepEndMessage.Fields.language);
				String message = json.getAsString(JbEntityBotCommandStepEndMessage.Fields.message);
				endMessages.put(language, message);
			}
			response.put(status, endMessages);
		}	
		
		return response;
	}

	private Map<String, String> getExplanations(String stepName, CcpSelectUnionAll resultFromSearchAllSteps) {
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JbEntityBotCommandStepExplanation.ENTITY);
		List<CcpJsonRepresentation> collect = entityRows.stream().filter(x -> x.getAsString(JbEntityBotCommandStepExplanation.Fields.stepName).equals(stepName)).collect(Collectors.toList());
		Map<String, String> response = new HashMap<>();
		for (CcpJsonRepresentation json : collect) {
			String language = json.getAsString(JbEntityBotCommandStepExplanation.Fields.language);
			String message = json.getAsString(JbEntityBotCommandStepExplanation.Fields.message);
			response.put(language, message);
			
		}
		return response;
	}

	private Map<String, String> getStartMessages(String stepName, CcpSelectUnionAll resultFromSearchAllSteps) {
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JbEntityBotCommandStepStartMessage.ENTITY);
		List<CcpJsonRepresentation> collect = entityRows.stream().filter(x -> x.getAsString(JbEntityBotCommandStepStartMessage.Fields.stepName).equals(stepName)).collect(Collectors.toList());
		Map<String, String> response = new HashMap<>();
		for (CcpJsonRepresentation json : collect) {
			String language = json.getAsString(JbEntityBotCommandStepStartMessage.Fields.language);
			String message = json.getAsString(JbEntityBotCommandStepStartMessage.Fields.message);
			response.put(language, message);
			
		}
		return response;
	}

	private Map<Integer, String> getNextSteps(CcpJsonRepresentation entityRow) {
		Map<Integer, String> nextSteps = new HashMap<>();
		List<CcpJsonRepresentation> asJsonList = entityRow.getAsJsonList(JbEntityBotCommandStep.Fields.nextStep);
		for (CcpJsonRepresentation json : asJsonList) {
			Integer status = json.getAsIntegerNumber(Fields.status);
			String stepName = json.getAsString(JbEntityBotCommandStep.Fields.stepName);
			nextSteps.put(status, stepName);
		}
		return nextSteps;
	}
}
