package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.instant.messenger.CcpInstantMessenger;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
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
	
	private static final JbBotEngine INSTANCE = new JbBotEngine();
	
	
	private final Map<String, Bot> allBots;

	private final Map<String, BotCommand> allCommands;

	private final Map<String, JbBotBusiness> allSteps;
	
	private JbBotEngine() {
		JnSystemProperties systemProperties = new JnSystemProperties();
		
		JbBotType[] bots = JbBotType.values();
		
		CcpJsonRepresentation[] parametersToSearchBots = new CcpJsonRepresentation[bots.length];
		List<String> languages = systemProperties.languages();
		
		int k = 0;
		for (JbBotType bot : bots) {
			for (var language : languages) {
				CcpJsonRepresentation parameterToSearchBots = bot.getParameterToSearchBot();
				parametersToSearchBots[k++] = parameterToSearchBots
						.put(JbEntityBotExplanation.Fields.language, language)
						;
			}
		}
		
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll resultFromSearchBots = crud.unionAll(parametersToSearchBots, JnDeleteKeysFromCache.INSTANCE, JbEntityBot.ENTITY, JbEntityBotExplanation.ENTITY);
		Map<String, Bot> allBots = new HashMap<>();
		
		for (JbBotType botType : bots) {
			String name = botType.name();
			Bot bot = new Bot(name, resultFromSearchBots);
			allBots.put(name, bot);
		}
		
		this.allBots = allBots;
		
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
		
		Map<String, BotCommand> allCommands = new HashMap<>();
		
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
			allCommands.put(commandName, botCommand);
		}
		
		this.allCommands = allCommands;
		
		CcpJsonRepresentation[] parametersToSearchAllSteps = allSteps.toArray(new CcpJsonRepresentation[allSteps.size()]);
		
		CcpSelectUnionAll resultFromSearchAllSteps = crud.unionAll(parametersToSearchAllSteps, JnDeleteKeysFromCache.INSTANCE, 
				JbEntityBotCommandStepStartMessage.ENTITY, 
				JbEntityBotCommandStepEndMessage.ENTITY, 
				JbEntityBotCommandStep.ENTITY 
				);
		
		Map<String, JbBotBusiness> stepsMap = new HashMap<>();
		
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JbEntityBotCommandStep.ENTITY);
		
		for (CcpJsonRepresentation entityRow : entityRows) {
			String engine = entityRow.getAsString(JbEntityBotCommandStep.Fields.engine);
			String stepName = entityRow.getAsString(JbEntityBotCommandStep.Fields.stepName);
			BotCommandStep step = new BotCommandStep(stepName, engine, resultFromSearchAllSteps);
			stepsMap.put(stepName, step);
		}
		
		CommonsBotCommandStep[] values = CommonsBotCommandStep.values();
		for (CommonsBotCommandStep value : values) {
			String name = value.name();
			BotCommandStep botCommandStep = value.getBotCommandStep(resultFromSearchAllSteps);
			stepsMap.put(name, botCommandStep);
		}
		this.allSteps = stepsMap;
	}

	
	public static enum JbBotType implements CcpJsonFieldName{
		user, support;
		
		private CcpJsonRepresentation getParameterToSearchBot() {
			String botName =  this.name();
			var parameterToSearchBots = CcpOtherConstants.EMPTY_JSON
					.put(JbEntityBot.Fields.botName, botName)
			;
			return parameterToSearchBots;
		}
		
		public Bot getBot() {
			Bot bot = JbBotEngine.INSTANCE.allBots.get(this.name());
			return bot;
		}
	}
	
	public static interface JbBotBusiness extends CcpBusiness, CcpJsonFieldName{
		default boolean isVisible(CcpJsonRepresentation json) {
			return true;
		}
		
		default boolean hasPriority(CcpJsonRepresentation json) {
			return false;
		}
		
		default Bot getBot(CcpJsonRepresentation json) {
			String name = json.getAsString(JbEntityBot.Fields.botName);
			var response = JbBotEngine.INSTANCE.allBots.get(name);
			return response;
		}

		default JbBotBusiness getLoadedCommand(CcpJsonRepresentation json) {
			JbBotBusiness response = this.getBot(json).getCommand(json);
			return response;
		}
		
		default Map<String, String> loadLabelsWithLanguages(String filterValue, CcpSelectUnionAll resultFromSearchAllSteps, CcpEntity entity, CcpJsonFieldName filterField, CcpJsonFieldName languageField, CcpJsonFieldName messageField) {
			List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(entity);
			List<CcpJsonRepresentation> collect = entityRows.stream().filter(x -> x.getAsString(filterField).equals(filterValue)).collect(Collectors.toList());
			Map<String, String> response = new HashMap<>();
			for (CcpJsonRepresentation json : collect) {
				String language = json.getAsString(languageField);
				String message = json.getAsString(messageField);
				response.put(language, message);
			}
			return response;
		}
		
		default String getExplanation(CcpJsonRepresentation json) {
			return "";
		}
		
		default boolean hasExplanation(CcpJsonRepresentation json) {
			return false;
		}
		
		default String getIdentifier(CcpJsonRepresentation json) {
			String name = this.name();
			return name;
		}
	}
	
	private static enum CommonsBotCommandStep implements JbBotBusiness{
		
			saveSession{

				public CcpJsonRepresentation apply(CcpJsonRepresentation newJson) {
					CcpJsonRepresentation removeFields = newJson.removeFields(JbEntityBotCommandStepSession.Fields.commandName, JbEntityBotCommandStepSession.Fields.stepName);
					CcpJsonRepresentation put = newJson.put(JbEntityBotCommandStepSession.Fields.json, removeFields);
					JbEntityBotCommandStepSession.ENTITY.save(put);
					return newJson;
				}
				
				public boolean isVisible(CcpJsonRepresentation json) {
					return false;
				}
				
				public boolean hasPriority(CcpJsonRepresentation json) {
					return false;
				}
			},
		
			removeSession{
				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					JbEntityBotCommandStepSession.ENTITY.delete(json);
					CcpJsonRepresentation removeFields = json.removeFields(Fields.stepName, Fields.commandName);
					return removeFields;
				}				
				
			},
			chatId{
				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);
					
					Bot bot = this.getBot(json);
					json = bot.sendMessage(json, "" + chatId);
					return json;
				}
				
				
			},
			showAllCommands{
				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					
					Bot bot = this.getBot(json);
					String listedCommands = bot.getAllCommands(json)
							.stream()
							.filter(x -> x.isVisible(json))
							.map(x -> x.getIdentifier(json))
							.collect(Collectors.toList())
							.toString()
							.replace("[", "")
							.replace("]", "")
							.replace(",", ", ")
							;
					CcpJsonRepresentation sendMessage = bot.sendMessage(json, listedCommands);
					return sendMessage;
				}
				
			},
			explainThisBot{
				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					var bot = this.getBot(json);
					String explanation = bot.getExplanation(json);
					CcpJsonRepresentation sendMessage = bot.sendMessage(json, explanation);
					return sendMessage;
				}
				
				public boolean isVisible(CcpJsonRepresentation json) {
					Bot bot = this.getBot(json);
					boolean hasExplanation = bot.hasExplanation(json);
					return hasExplanation;
				}
			},
			explainThisCommand{
				public boolean isVisible(CcpJsonRepresentation json) {
					boolean commandLess = false == json.containsAllFields(Fields.commandName);
					if(commandLess) {
						return false;
					}
					
					JbBotBusiness command = this.getLoadedCommand(json);
					boolean hasExplanation = command.hasExplanation(json);
					return hasExplanation;
				}

				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					JbBotBusiness command = this.getLoadedCommand(json);
					String explanation = command.getExplanation(json);
					
					Bot bot = this.getBot(json);
					CcpJsonRepresentation sendMessage = bot.sendMessage(json, explanation);
					return sendMessage;
				}
			}
		;
		private BotCommandStep getBotCommandStep(CcpSelectUnionAll result) {
			BotCommandStep response = new BotCommandStep(this.name(), this, result);
			return response;
		}

			public boolean hasPriority(CcpJsonRepresentation json) {
				return true;
			}
	}
	
	private static enum Fields implements CcpJsonFieldName{
		bots, token,  replyTo, language, stepName, status, commandName, typedValue
	}
	
	private static class Bot implements JbBotBusiness{
		private final String name;
		private final List<String> commands;
		private final Map<String, String> explanations;

		private Bot(String name, CcpSelectUnionAll resultFromSearchBots) {
			this.explanations = this.loadLabelsWithLanguages(name, resultFromSearchBots, JbEntityBotExplanation.ENTITY, JbEntityBotExplanation.Fields.botName, JbEntityBotExplanation.Fields.language, JbEntityBotExplanation.Fields.message);
			this.commands = this.loadCommands(name, resultFromSearchBots);
			this.name = name;
		}
		
		private List<String> loadCommands(String name, CcpSelectUnionAll resultFromSearchBots){
			JbBotType valueOf = JbBotType.valueOf(name);
			CcpJsonRepresentation parameterToSearchBot = valueOf.getParameterToSearchBot();
			CcpJsonRepresentation recordFromUnionAll = JbEntityBot.ENTITY.getRecordFromUnionAll(resultFromSearchBots, parameterToSearchBot);
			List<String> commands = new ArrayList<>(recordFromUnionAll.getAsStringList(JbEntityBot.Fields.commandName));
			CommonsBotCommandStep[] values = CommonsBotCommandStep.values();
			List<String> commonsCommands = Arrays.asList(values).stream().map(x -> x.name()).collect(Collectors.toList());
			commands.addAll(commonsCommands);
			return commands;
		}
		
		public String toString() {
			return this.name;
		}
		
		private JbBotBusiness getCommand(CcpJsonRepresentation json) {
			
			CcpJsonRepresentation commandData = this.getCommandData(json);
			
			String commandName = commandData.getAsString(JbEntityBotCommandStepSession.Fields.commandName);
			
			boolean commandWasFound = false == commandName.trim().isEmpty();
			
			if(commandWasFound) {
				JbBotBusiness command = this.getCommand(json, commandName);
				return command;
			}

			String typedValue = json.getAsString(Fields.typedValue);
			JbBotBusiness command = this.getCommand(json, typedValue);
			
			boolean hasPriority = command.hasPriority(json);
			
			if(hasPriority) {
				return command;
			}
			
			CcpJsonRepresentation sessionToSave = json.duplicateValueFromField(Fields.typedValue, JbEntityBotCommandStepSession.Fields.stepName, JbEntityBotCommandStepSession.Fields.commandName);
			CommonsBotCommandStep.saveSession.apply(sessionToSave);
			return command;
		}

		private JbBotBusiness getCommand(CcpJsonRepresentation json, String commandName) {
			Optional<BotCommand> findFirst = JbBotEngine.INSTANCE.allCommands.values().stream()
			.filter(x -> x.getIdentifier(json).equals(commandName))
			.filter(x -> x.isVisible(json))
			.findFirst();
			
			boolean commandNotFound = false == findFirst.isPresent();
		
			if(commandNotFound) {
				return CommonsBotCommandStep.showAllCommands;
			}

			BotCommand botCommand = findFirst.get();
			return botCommand;
		}

		private CcpJsonRepresentation getCommandData(CcpJsonRepresentation json) {
			
			String typedValue = json.getAsString(Fields.typedValue);
			
			Optional<BotCommand> findFirst = JbBotEngine.INSTANCE.allCommands.values().stream()
			.filter(x -> x.getIdentifier(json).equals(typedValue))
			.filter(x -> x.isVisible(json))
			.filter(x -> x.hasPriority(json))
			.findFirst();
			
			
			boolean priorityCommandWillBePerformedRighNow = findFirst.isPresent();
			
			if(priorityCommandWillBePerformedRighNow) {
				BotCommand botCommand = findFirst.get();
				CcpJsonRepresentation put = json
						.put(JbEntityBotCommandStepSession.Fields.commandName, botCommand.name)
						.put(JbEntityBotCommandStepSession.Fields.stepName, botCommand.name);
				return put;
			}
			

			boolean isPerformingCommandRightNow = json.containsAllFields(Fields.commandName, Fields.stepName);
			
			if(isPerformingCommandRightNow) {
				String commandName = json.getAsString(Fields.commandName);
				CcpJsonRepresentation put = json
						.put(JbEntityBotCommandStepSession.Fields.commandName, commandName)
						.put(JbEntityBotCommandStepSession.Fields.stepName, commandName);
				return put;
			}	

			CcpEntityMetaData entityMetaData = JbEntityBotCommandStepSession.ENTITY.getEntityMetaData();
			
			CcpJsonRepresentation oneById = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(json, CcpOtherConstants.RETURNS_EMPTY_JSON);
			
			CcpJsonRepresentation innerJson = oneById.getInnerJson(JbEntityBotCommandStepSession.Fields.json);
			
			CcpJsonRepresentation mergeWithAnotherJson = innerJson.mergeWithAnotherJson(json);

			return mergeWithAnotherJson;
		}
		
		private CcpJsonRepresentation sendMessage(CcpErrorFlowDisturb e, Predicate<CcpErrorFlowDisturb> condition, Function<CcpErrorFlowDisturb, Map<String, String>> messagesProducer) {
			
			boolean conditionDoesNotMatch = false == condition.test(e);
			
			if(conditionDoesNotMatch) {
				return e.json;
			}
			
			CcpJsonRepresentation json = e.json;
			Map<String, String> messages = messagesProducer.apply(e);
			CcpJsonRepresentation sendMessage = this.sendMessage(json, messages);
			return sendMessage;
		}

		private CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, Map<String, String> messages) {
			String language = json.getAsString(Fields.language);
			String message = messages.get(language);
			boolean hasNotMessage = false == messages.containsKey(language);

			if(hasNotMessage) {
				return json;
			}
			CcpJsonRepresentation mergeWithAnotherJson = this.sendMessage(json, message);
			return mergeWithAnotherJson;
		}

		private CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, String message) {
			JnSystemProperties systemProperties = new JnSystemProperties();
			CcpInstantMessenger instantMessenger = CcpDependencyInjection.getDependency(CcpInstantMessenger.class);
			
			Bot bot = this.getBot(json);
			
			String botToken = systemProperties.getSystemInnerProperty(Fields.bots, bot, Fields.token) ;
			Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);
			Long replyTo = json.getAsLongNumber(Fields.replyTo);
			
			CcpJsonRepresentation sendTextMessage = instantMessenger.sendTextMessage(botToken, chatId, replyTo, message);
			CcpJsonRepresentation mergeWithAnotherJson = json.mergeWithAnotherJson(sendTextMessage);
			return mergeWithAnotherJson;
		}

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			
			CcpJsonRepresentation put = json.put(JbEntityBotCommandStepSession.Fields.botName, this.name);

			JbBotBusiness currentCommand = this.getCommand(json);
			
			CcpJsonRepresentation apply = currentCommand.apply(put);
			
			return apply;
		} 	
		
		public String name() {
			return this.name;
		}
		
		public boolean hasExplanation(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			boolean response = this.explanations.containsKey(language);
			return response;
		}

		public String getExplanation(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			String response = this.explanations.get(language);
			return response;
		}
		
		private List<JbBotBusiness> getAllCommands(CcpJsonRepresentation json){
			List<JbBotBusiness> collect = this.commands.stream().map(x -> this.getCommand(json)).collect(Collectors.toList());
			return collect;
		}
		
	}
	private static class BotCommand implements JbBotBusiness{
		
		private final String name;	
		private final Map<String, String> names;
		private final Map<String, String> explanations;
		
		private BotCommand(String name, CcpSelectUnionAll result) {

			this.explanations = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandExplanation.ENTITY, JbEntityBotCommandExplanation.Fields.commandName, JbEntityBotCommandExplanation.Fields.language, JbEntityBotCommandExplanation.Fields.message);
			this.names = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandName.ENTITY, JbEntityBotCommandName.Fields.commandName, JbEntityBotCommandName.Fields.language, JbEntityBotCommandName.Fields.message);
			this.name = name;
		
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

			String stepName = json.getAsString(Fields.stepName);
			
			JbBotBusiness botCommandStep = JbBotEngine.INSTANCE.allSteps.get(stepName);
			
			CcpJsonRepresentation apply = botCommandStep.apply(json);

			return apply;
		}
		
		public boolean hasExplanation(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			boolean response = this.explanations.containsKey(language);
			return response;
		}

		public String getExplanation(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			String response = this.explanations.get(language);
			return response;
		}

		public String getIdentifier(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			String response = "/" + this.names.getOrDefault(language, this.name);
			return response;
		}

		public boolean isVisible(CcpJsonRepresentation json) {
			
			JbBotBusiness firstStep = JbBotEngine.INSTANCE.allSteps.get(this.name);
			
			boolean listed = firstStep.isVisible(json);
			
			return listed;
		}
		
		public String name() {
			return this.name;
		}
	}
	
	private static class BotCommandStep implements JbBotBusiness{

		private final String name;
		private final CcpBusiness engine;
		private final Map<Integer, String> nextSteps;
		private final Map<String, String> explanations;
		private final Map<String, String> startMessages;
		private final Map<Integer, Map<String, String>> endMessages;
		
		private BotCommandStep(String name, String engine, CcpSelectUnionAll result) {
			this(name, (CcpBusiness)new CcpStringDecorator(engine).reflection().newInstance(), result);
		}

		private BotCommandStep(String name, CcpBusiness engine, CcpSelectUnionAll result) {

			this.name = name;
			this.engine = engine;
			this.nextSteps = this.getNextSteps(name, result);
			this.endMessages = this.getEndMessages(name, result);
			this.explanations = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandStepExplanation.ENTITY, JbEntityBotCommandStepExplanation.Fields.stepName, JbEntityBotCommandStepExplanation.Fields.language, JbEntityBotCommandStepExplanation.Fields.message);
			this.startMessages = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandStepStartMessage.ENTITY, JbEntityBotCommandStepStartMessage.Fields.stepName, JbEntityBotCommandStepStartMessage.Fields.language, JbEntityBotCommandStepStartMessage.Fields.message);
		}
		
		private Map<Integer, String> getNextSteps(String name, CcpSelectUnionAll result) {
			Map<Integer, String> nextSteps = new HashMap<>();
			CcpJsonRepresentation parametersToSearch = CcpOtherConstants.EMPTY_JSON.put(JbEntityBotCommandStep.Fields.stepName, name);
			CcpJsonRepresentation entityRow = JbEntityBotCommandStep.ENTITY.getRecordFromUnionAll(result, parametersToSearch);
			List<CcpJsonRepresentation> asJsonList = entityRow.getAsJsonList(JbEntityBotCommandStep.Fields.nextStep);
			for (CcpJsonRepresentation json : asJsonList) {
				Integer status = json.getAsIntegerNumber(Fields.status);
				String stepName = json.getAsString(JbEntityBotCommandStep.Fields.stepName);
				nextSteps.put(status, stepName);
			}
			return nextSteps;
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
			Bot bot = this.getBot(json);	
			try {
				json = bot.sendMessage(json, this.startMessages);
				CcpJsonRepresentation apply = this.engine.execute(json);
				CcpJsonRepresentation closeSession = CommonsBotCommandStep.removeSession.apply(apply);
				return closeSession;
			} catch(CcpJsonValidationError e) {
				json = bot.sendMessage(json, this.explanations);
				return json;
			} catch (CcpErrorFlowDisturb e) {
				
				json = bot.sendMessage(e, ex -> this.endMessages.containsKey(ex.status.asNumber()), ex ->  this.endMessages.get(ex.status.asNumber()));
				
				Predicate<CcpErrorFlowDisturb> evaluateIfHasMoreSession = ex -> this.nextSteps.containsKey(ex.status.asNumber());
				
				Function<CcpErrorFlowDisturb, CcpJsonRepresentation> updateSession = ex -> {
					int asNumber = ex.status.asNumber();
					String nextStep = this.nextSteps.get(asNumber);
					CcpJsonRepresentation newJson = ex.json.put(JbEntityBotCommandStepSession.Fields.stepName, nextStep);
					CommonsBotCommandStep.saveSession.apply(newJson);
					return newJson;
				};
				
				Function<CcpErrorFlowDisturb, CcpJsonRepresentation> removeSession = ex -> CommonsBotCommandStep.removeSession.apply(ex.json);
				
				CcpJsonRepresentation result = this.execute(e, evaluateIfHasMoreSession, updateSession, removeSession);
				return result;
			}
		}

		private CcpJsonRepresentation execute(CcpErrorFlowDisturb e, Predicate<CcpErrorFlowDisturb> condition, Function<CcpErrorFlowDisturb, CcpJsonRepresentation> jsonProducerIfConditionMatches, Function<CcpErrorFlowDisturb, CcpJsonRepresentation> jsonProducerIfConditionDoesNotMatch) {

			boolean conditionMatches = condition.test(e);
			
			if(conditionMatches) {
				CcpJsonRepresentation apply = jsonProducerIfConditionMatches.apply(e);
				return apply;
			}
			CcpJsonRepresentation apply = jsonProducerIfConditionDoesNotMatch.apply(e);
			return apply;
		}
		
		public String name() {
			return this.name;
		}
	}
	
}
