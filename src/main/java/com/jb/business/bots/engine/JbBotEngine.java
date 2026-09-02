package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpReflectionConstructorDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.global.engine.CcpJsonValidatorEngine.CcpJsonValidationError;
import com.jb.business.bots.login.token.JbBotSolveLoginTokenTicket.StepFields;
import com.jb.entities.JbEntityBot;
import com.jb.entities.JbEntityBotAllowedUser;
import com.jb.entities.JbEntityBotCommand;
import com.jb.entities.JbEntityBotCommandExplanation;
import com.jb.entities.JbEntityBotCommandName;
import com.jb.entities.JbEntityBotCommandStep;
import com.jb.entities.JbEntityBotCommandStepEndMessage;
import com.jb.entities.JbEntityBotCommandStepExplanation;
import com.jb.entities.JbEntityBotCommandStepFlowMessage;
import com.jb.entities.JbEntityBotCommandStepSession;
import com.jb.entities.JbEntityBotCommandStepStartMessage;
import com.jb.entities.JbEntityBotExplanation;
import com.jn.business.messages.JnBusinessSendInstantMessage;
import com.jn.business.messages.JnBusinessSendInstantMessage.JnInstantMessageType;
import com.jn.business.messages.JnBusinessSendInstantMessage.JnJsonValidator;
import com.jn.business.messages.JnBusinessSendInstantMessage.JnMessageTextJsonValidator;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.jn.utils.JnSystemProperties;

/**
 * Motor central dos bots de suporte Telegram do jobsnow. Inicializado como singleton,
 * carrega do Elasticsearch toda a configuração dos bots (tipos, comandos, passos, mensagens,
 * usuários permitidos e explicações) e gerencia o fluxo de interação multi-passo: receber
 * texto do usuário, identificar o bot e o comando, avançar a sessão e enviar respostas.
 */
public class JbBotEngine {
	
	private static final JbBotEngine INSTANCE = new JbBotEngine();

	private final Map<String, BotCommand> allCommands = new HashMap<>();
	
	private final Map<String, JbBotBusiness> allSteps;

	private final Map<CcpJsonFieldName, Bot> allBots;
	
	private JbBotEngine() {
		
		JbBotType[] bots = JbBotType.values();
		
		CcpJsonRepresentation[] parametersToSearchBots = new CcpJsonRepresentation[bots.length * JnLanguage.values().length];
	
		var languages = JnLanguage.values();
		
		int k = 0;
		for (JbBotType bot : bots) {
			for (var language : languages) {
				Supplier<CcpJsonRepresentation> parameterToSearchBot = bot.getParameterToSearchBot();
				CcpJsonRepresentation parameterToSearchBots = parameterToSearchBot.get();
				parametersToSearchBots[k++] = parameterToSearchBots
						.put(JbEntityBotExplanation.Fields.language, language)
						; 
			}
		}
		
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll resultFromSearchBots = crud.unionAll(parametersToSearchBots, JnDeleteKeysFromCache.INSTANCE, 
				JbEntityBotAllowedUser.ENTITY,
				JbEntityBotExplanation.ENTITY, 
				JbEntityBot.ENTITY
		);
		Map<CcpJsonFieldName, Bot> allBots = new HashMap<>();
		
		for (JbBotType botType : bots) {
			Bot bot = new Bot(botType, resultFromSearchBots);
			allBots.put(botType, bot);
		}
		
		this.allBots = allBots;
		
		k = 0;
		
		List<CcpJsonRepresentation> list = new ArrayList<>()
				;
		for (CcpJsonRepresentation parameterToSearchBot : parametersToSearchBots) {
			Supplier<CcpJsonRepresentation> jsonSupplier = parameterToSearchBot.getJsonSupplier();
			CcpJsonRepresentation recordFromUnionAll = JbEntityBot.ENTITY.getRecordFromUnionAll(resultFromSearchBots, jsonSupplier);
			List<String> commands = recordFromUnionAll.getAsStringList(JbEntityBot.Fields.commandName);
			
			String language = parameterToSearchBot.getAsString(JbEntityBotExplanation.Fields.language);
			
			for (String command : commands) {

					CcpJsonRepresentation parameterToSearchCommandsAndFirstSteps = 
							recordFromUnionAll
						.putSameValueInManyFields(command, JbEntityBotCommandStep.Fields.stepName, JbEntityBotCommand.Fields.commandName)
						.put(JbEntityBotExplanation.Fields.language, language)
						;

					list.add(parameterToSearchCommandsAndFirstSteps);
				}
		}
		
		
		CcpJsonRepresentation[] parametersToSearchCommandsAndFirstSteps = list.toArray(new CcpJsonRepresentation[list.size()]);

		CcpSelectUnionAll resultFromSearchCommandsAndFirstSteps = crud.unionAll(parametersToSearchCommandsAndFirstSteps, JnDeleteKeysFromCache.INSTANCE, 
				JbEntityBotCommandStepExplanation.ENTITY, 
				JbEntityBotCommandExplanation.ENTITY, 
				JbEntityBotCommandStep.ENTITY, 
				JbEntityBotCommandName.ENTITY, 
				JbEntityBotCommand.ENTITY 
				);
		
		Map<String, CcpJsonRepresentation> allSteps = new HashMap<>();
		List<CcpJsonRepresentation> firstSteps = resultFromSearchCommandsAndFirstSteps.getEntityRows(JbEntityBotCommandStep.ENTITY);
		
		for (CcpJsonRepresentation firstStep : firstSteps) {
			String stepName = firstStep.getAsString(JbEntityBotCommandStep.Fields.stepName);
			for (var language : languages) {
				CcpJsonRepresentation putLanguage = firstStep
						.duplicateValueFromField(JbEntityBotCommandStep.Fields.stepName, JbEntityBotCommand.Fields.commandName)
						.put(JbEntityBotExplanation.Fields.language, language)
						;
				allSteps.put(stepName, putLanguage);
			}
			var nextSteps = firstStep.getAsJsonList(JbEntityBotCommandStep.Fields.stepFlow);
			for (CcpJsonRepresentation nextStep : nextSteps) {
				for (var language : languages) {
					CcpJsonRepresentation duplicateValueFromField = nextStep
							.duplicateValueFromField(JbEntityBotCommandStep.Fields.stepName, JbEntityBotCommand.Fields.commandName)
							;
					CcpJsonRepresentation put = duplicateValueFromField
							.put(JbEntityBotExplanation.Fields.language, language);
					allSteps.put(stepName, put);
				}
			}
			CcpJsonRepresentation duplicateValueFromField = firstStep.duplicateValueFromField(JbEntityBotCommandStep.Fields.stepName, JbEntityBotCommand.Fields.commandName);
			String commandName = duplicateValueFromField.getAsString(JbEntityBotCommand.Fields.commandName);
			BotCommand botCommand = new BotCommand(commandName, resultFromSearchCommandsAndFirstSteps);
			this.allCommands.put(commandName, botCommand);
		}

		JbDefaultBotCommandStep[] defaultBotCommandSteps = JbDefaultBotCommandStep.values();
	
		for (JbDefaultBotCommandStep defaultBotCommandStep : defaultBotCommandSteps) {
			String commandName = defaultBotCommandStep.name();
			BotCommand botCommand = new BotCommand(commandName, resultFromSearchCommandsAndFirstSteps);
			this.allCommands.put(commandName, botCommand);

			var step = CcpOtherConstants.EMPTY_JSON.putSameValueInManyFields(commandName,
					JbEntityBotCommandStep.Fields.stepName, JbEntityBotCommand.Fields.commandName);
			allSteps.put(commandName, step);
		}
		
		Collection<CcpJsonRepresentation> parametersToSearchSteps = allSteps.values();
		CcpJsonRepresentation[] parametersToSearchAllSteps = new CcpJsonRepresentation[parametersToSearchSteps.size() * JnLanguage.values().length];
		k = 0;
		
		for (CcpJsonRepresentation step : parametersToSearchSteps) {
			for (var language : languages) {
				CcpJsonRepresentation parameterToSearchAllSteps = step.put(JbEntityBotExplanation.Fields.language, language);
				parametersToSearchAllSteps[k++] = parameterToSearchAllSteps;
			}
		}
	
		CcpSelectUnionAll resultFromSearchAllSteps = crud.unionAll(parametersToSearchAllSteps, JnDeleteKeysFromCache.INSTANCE, 
				JbEntityBotCommandStepStartMessage.ENTITY, 
				JbEntityBotCommandStepFlowMessage.ENTITY, 
				JbEntityBotCommandStepEndMessage.ENTITY,
				JbEntityBotCommandExplanation.ENTITY, 
				JbEntityBotCommandStep.ENTITY 
				);
		
		Map<String, JbBotBusiness> stepsMap = new HashMap<>(); 
		
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JbEntityBotCommandStep.ENTITY);
		
		for (CcpJsonRepresentation entityRow : entityRows) {
			String stepName = entityRow.getAsString(JbEntityBotCommandStep.Fields.stepName);
			BotCommandStep step = new BotCommandStep(stepName, resultFromSearchAllSteps);
			stepsMap.put(stepName, step);
		}
		
		for (JbDefaultBotCommandStep value : defaultBotCommandSteps) {
			String name = value.name();
			BotCommandStep botCommandStep = value.getBotCommandStep(resultFromSearchAllSteps);
			stepsMap.put(name, botCommandStep);
		}
		this.allSteps = stepsMap;
	}
	
	public static enum JbBotType implements CcpJsonFieldName{
		user {
			@Override
			protected boolean isRestricted() {
				return false;
			}
		}, support {
			protected boolean isRestricted() {
				return true;
			}
		};
		
		private Supplier<CcpJsonRepresentation> getParameterToSearchBot() {
			String botName =  this.name();
			var parameterToSearchBots = CcpOtherConstants.EMPTY_JSON
					.put(JbEntityBot.Fields.botName, botName)
			;
			Supplier<CcpJsonRepresentation> jsonSupplier = parameterToSearchBots.getJsonSupplier();
			return jsonSupplier;
		}
		
		public Bot getBot() {
			Bot bot = JbBotEngine.INSTANCE.allBots.get(this);
			return bot;
		}
		
		abstract protected boolean isRestricted();
	}
	
	public static interface JbBotBusiness extends CcpBusiness{
		default String name() {
			return this.getClass().getName();
		}
		
		default boolean isVisible(CcpJsonRepresentation json) {
			return true;
		}
		
		default boolean hasPriority(CcpJsonRepresentation json) {
			return false;
		}
		
		default Bot getBot(CcpJsonRepresentation json) {
			String name = json.getAsString(JbEntityBot.Fields.botName);
			JbBotType botType = JbBotType.valueOf(name);
			var response = JbBotEngine.INSTANCE.allBots.get(botType);
			return response;
		} 

		default BotCommand getLoadedCommand(CcpJsonRepresentation json) {
			Bot bot = this.getBot(json);
			BotCommand response = bot.getCommand(json);
			return response;
		}
		
		default List<CcpJsonRepresentation> loadLabelsWithLanguages(String filterValue, CcpSelectUnionAll resultFromSearchAllSteps, CcpEntity entity, CcpJsonFieldName filterField, CcpJsonFieldName languageField, CcpJsonFieldName messageField) {
			List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(entity);
			List<CcpJsonRepresentation> response = entityRows.stream().filter(x -> x.getAsString(filterField).equals(filterValue)).collect(Collectors.toList());
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
		
		default CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, String message) {
			
			CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.message, message);
			CcpJsonRepresentation putToken = this.putToken(json);
			CcpJsonRepresentation sendMessage = JnInstantMessageType.text.sendMessage(putToken, put);
			return sendMessage;
		}

		default CcpJsonRepresentation putToken(CcpJsonRepresentation json) {
			CcpJsonFieldName botName = json.getAsStringDecorator(JnJsonValidator.botName).jsonFieldName();
			String botToken =  JnSystemProperties.INSTANCE.getSystemInnerProperty(Fields.bots, botName);
			
			
			CcpJsonRepresentation putToken = json.put(JnMessageTextJsonValidator.botToken, botToken);
			return putToken;
		}
	}
	
	public static enum JbDefaultBotCommandStep implements JbBotBusiness{
		
			removeSession{
				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					JbEntityBotCommandStepSession.ENTITY.delete(json);
					BotCommand loadedCommand = this.getLoadedCommand(json);
					loadedCommand.removeSession(json);
					return json;
				}				
			},
			chatId{
				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);
					json = super.sendMessage(json, "" + chatId);
					return json;
				}
			},
			showAllCommands{
				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					
					Bot bot = this.getBot(json);
					List<JbBotBusiness> allCommands = bot.getAllCommands(json);
					List<String> collect = new ArrayList<String>();
					for (JbBotBusiness command : allCommands) {
 						boolean isInvisibleCommand = false == command.isVisible(json);
						if(isInvisibleCommand) {
							continue;
						}
						
						String identifier = command.getIdentifier(json);
						collect.add(identifier);
					}

					String listedCommands = collect
							.toString()
							.replace("[", "")
							.replace("]", "")
							.replace(",", ", ")
							;
					CcpJsonRepresentation sendMessage = super.sendMessage(json, listedCommands);
					return sendMessage;
				}
			},
			explainThisBot{
				public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
					var bot = this.getBot(json);
					String explanation = bot.getExplanation(json);
					CcpJsonRepresentation sendMessage = super.sendMessage(json, explanation);
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
					boolean commandLess = false == json.containsAllFields(JbEntityBotCommandStepSession.Fields.commandName);
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
					
					CcpJsonRepresentation sendMessage = super.sendMessage(json, explanation);
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
		bots, replyTo, language, status, commandParameters, message, message_id
	}
	
	private static class Bot implements JbBotBusiness{
		private final JbBotType botType;
		private final boolean isRestricted;
		private final List<String> commands;
		private final Set<Long> allowedUsers;
		private final List<CcpJsonRepresentation> explanations;

		private Bot(JbBotType botType, CcpSelectUnionAll resultFromSearchBots) {
			this.explanations = this.loadLabelsWithLanguages(botType.name(), resultFromSearchBots, JbEntityBotExplanation.ENTITY, JbEntityBotExplanation.Fields.botName, JbEntityBotExplanation.Fields.language, JbEntityBotExplanation.Fields.message);
			this.allowedUsers = this.loadAllowedUsers(botType, resultFromSearchBots);
			this.commands = this.loadCommands(botType, resultFromSearchBots);
			this.isRestricted = botType.isRestricted();
			this.botType = botType;
			
		}
		
		private Set<Long> loadAllowedUsers(JbBotType valueOf, CcpSelectUnionAll resultFromSearchBots) {

			Supplier<CcpJsonRepresentation> parameterToSearchBot = valueOf.getParameterToSearchBot();
			CcpJsonRepresentation recordFromUnionAll = JbEntityBotAllowedUser.ENTITY.getRecordFromUnionAll(resultFromSearchBots, parameterToSearchBot);
			List<String> asStringList = recordFromUnionAll.getAsStringList(JbEntityBotAllowedUser.Fields.allowedUser);
			Set<Long> collect = asStringList.stream().map(x -> Double.valueOf(x).longValue()).collect(Collectors.toSet());
			return collect;
		}

		private List<String> loadCommands(JbBotType valueOf, CcpSelectUnionAll resultFromSearchBots){
			Supplier<CcpJsonRepresentation> jsonSupplier = valueOf.getParameterToSearchBot();
			CcpJsonRepresentation recordFromUnionAll = JbEntityBot.ENTITY.getRecordFromUnionAll(resultFromSearchBots, jsonSupplier);
			List<String> commands = new ArrayList<>(recordFromUnionAll.getAsStringList(JbEntityBot.Fields.commandName));
			JbDefaultBotCommandStep[] values = JbDefaultBotCommandStep.values();
			List<String> commonsCommands = Arrays.asList(values).stream().map(x -> x.name()).collect(Collectors.toList());
			commands.addAll(commonsCommands);
			return commands;
		}
		
		public String toString() {
			return this.name();
		}
		
		
		private CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, List<CcpJsonRepresentation> messages) {
			
			String language = json.getAsString(JbEntityBotCommandExplanation.Fields.language);
			String stepName = json.getAsString(JbEntityBotCommandStep.Fields.stepName);
			
			Optional<CcpJsonRepresentation> findFirst = messages.stream()
					.filter(x -> x.getAsString(JbEntityBotCommandStep.Fields.stepName).equals(stepName))
					.filter(x -> x.getAsString(JbEntityBotCommandExplanation.Fields.language).equals(language))
					.findFirst();
			
			boolean hasNoMessages = false == findFirst.isPresent();
			
			if(hasNoMessages) {
				return json;
			}
			
			CcpJsonRepresentation message = findFirst.get();

			String type = message.getOrDefault(JbEntityBotCommandStepStartMessage.Fields.instantMessageType, () -> JnInstantMessageType.text.name());
			
			CcpJsonRepresentation putToken = this.putToken(json);
			JnInstantMessageType valueOf = JnInstantMessageType.valueOf(type);
			CcpJsonRepresentation sendMessage = valueOf.sendMessage(putToken, message);
			
			return sendMessage;
			
		}
		
		public CcpJsonRepresentation loadSession(CcpJsonRepresentation json) {
			
			Collection<BotCommand> allCommands = JbBotEngine.INSTANCE.allCommands.values();
			
			for (BotCommand command : allCommands) {
				
				boolean commandNameDoesNotMatch = command.commandNameDoesNotMatch(json);
				
				if(commandNameDoesNotMatch) {
					continue;
				}
				
				boolean invisibleCommand = false == command.isVisible(json);
				
				if(invisibleCommand) {
					continue;
				}
				
				boolean hasPriority = command.hasPriority(json);
				
				if(hasPriority) {
					CcpJsonRepresentation priorityCommand = command.getCommandJson(json);
					return priorityCommand;
				}
				
				boolean hasNoSession = false == command.hasSession(json);
				
				if(hasNoSession) {
					json = command.getCommandJson(json);
					break;
				}
				
				CcpJsonRepresentation session = command.getSession(json);
				CcpJsonRepresentation innerJson = session.getInnerJson(JbEntityBotCommandStepSession.Fields.json);
				CcpJsonRepresentation mergeWithAnotherJson = innerJson.mergeWithAnotherJson(session);
				return mergeWithAnotherJson;
			}
			
			

			CcpEntityMetaData entityMetaData = JbEntityBotCommandStepSession.ENTITY.getEntityMetaData();
			
			CcpBusiness newSessionProducer = this.newSessionProducer(json);
			
			CcpJsonRepresentation savedSession = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(json, newSessionProducer);
			CcpJsonRepresentation innerJson = savedSession.getInnerJson(JbEntityBotCommandStepSession.Fields.json);
			CcpJsonRepresentation removeFields = savedSession.removeFields(JbEntityBotCommandStepSession.Fields.json);
			CcpJsonRepresentation handledSession = innerJson.mergeWithAnotherJson(removeFields);

			return handledSession;
		}

		public boolean isVisible(CcpJsonRepresentation json) {
			
			boolean openBot = false == this.isRestricted;
			if(openBot) {
				return true;
			}
			Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);

			boolean alloedUser = this.allowedUsers.contains(chatId);
			
			return alloedUser;
		}
		
		private CcpBusiness newSessionProducer(CcpJsonRepresentation json) {
			CcpBusiness newSessionProducer = jsn -> 
			json
			.mergeWithAnotherJson(json)
			.renameField(Fields.message, StepFields.typedValue)
			.put(JbEntityBotCommandStepSession.Fields.language, JnLanguage.portuguese)
			.getTransformedJson(JsonProducers.putCommandNameWhenHasNoSession)
			;
			return newSessionProducer;
		}
	
		public CcpJsonRepresentation apply(CcpJsonRepresentation message) {
			CcpJsonRepresentation json = message
					.put(JnBusinessSendInstantMessage.JnJsonValidator.botName, botType.name())
					//TODO PARAMETRIZAR ESSE TEXT
					.put(JnBusinessSendInstantMessage.JnJsonValidator.instantMessageType, JnInstantMessageType.text)
					.renameField(Fields.message_id, JnBusinessSendInstantMessage.Fields.replyTo)
					;

			CcpJsonRepresentation renameField = json.renameField(Fields.message, StepFields.typedValue);
			CcpJsonRepresentation loadSession = this.loadSession(renameField);
			BotCommand botCommand = this.getCommand(loadSession);
			CcpJsonRepresentation execute = botCommand.execute(loadSession);
			return execute;
		}

		protected BotCommand getCommand(CcpJsonRepresentation json) {
			String commandName = json.getAsString(JbEntityBotCommandStepSession.Fields.commandName);
			BotCommand botCommand = JbBotEngine.INSTANCE.allCommands.get(commandName);
			return botCommand;
		} 
		
		public String name() {
			return this.botType.name();
		}
		
		public boolean hasExplanation(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			Optional<String> findFirst = this.explanations.stream().filter(x -> x.getAsString(Fields.language).equals(language))
			.map(x -> x.getAsString(JbEntityBotExplanation.Fields.language))
			.findFirst();
			
			boolean response = findFirst.isPresent();
			return response;
		}

		public String getExplanation(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			Optional<String> findFirst = this.explanations.stream()
					.filter(x -> x.getAsString(Fields.language).equals(language))
					.filter(x -> x.getAsString(JbEntityBot.Fields.botName).equals(this.name()))
			.map(x -> x.getAsString(JbEntityBotExplanation.Fields.message))
			.findFirst();
			
			String response = findFirst.orElseThrow(() -> new RuntimeException("'" + language + "' is missing" ));
			return response;
		}
		
		private List<JbBotBusiness> getAllCommands(CcpJsonRepresentation json){
			List<JbBotBusiness> collect = this.commands.stream().map(x -> this.getCommand(x)).collect(Collectors.toList());
			return collect;
		}
		
		protected JbBotBusiness getCommand(String commandName) {
			Optional<BotCommand> findFirst = JbBotEngine.INSTANCE.allCommands.values().stream().filter(x -> x.name.equals(commandName)).findFirst();
			boolean commandNotFound = false == findFirst.isPresent();
		
			if(commandNotFound) {
				throw new RuntimeException("The command '" + commandName + "' whas not found");
			}
			
			BotCommand botCommand = findFirst.get();
			return botCommand;
		}
	}
	
	private static class BotCommand implements JbBotBusiness{
		
		private final String name;	
		private final List<String> parameterNames;
		private final List<CcpJsonRepresentation> names;
		private final List<CcpJsonRepresentation> explanations;
		private final Map<Long, CcpJsonRepresentation> sessions = new HashMap<>();
		
		private BotCommand(String name, CcpSelectUnionAll result) {

			this.explanations = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandExplanation.ENTITY, JbEntityBotCommandExplanation.Fields.commandName, JbEntityBotCommandExplanation.Fields.language, JbEntityBotCommandExplanation.Fields.message);
			this.names = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandName.ENTITY, JbEntityBotCommandName.Fields.commandName, JbEntityBotCommandName.Fields.language, JbEntityBotCommandName.Fields.message);
			this.parameterNames = this.loadParameterNames(name, result);
			this.name = name;
		}

		public CcpJsonRepresentation getCommandJson(CcpJsonRepresentation json) {
			CcpJsonRepresentation put = json
					.putSameValueInManyFields(this.name, JbEntityBotCommandStepSession.Fields.commandName, JbEntityBotCommandStepSession.Fields.stepName)
					.removeFields(JbEntityBotCommandStepSession.Fields.json)
					;
			return put;
		}

		public boolean commandNameDoesNotMatch(CcpJsonRepresentation json) {
			String typedValue = json.getAsString(StepFields.typedValue);
			String[] split = typedValue.split(" ");
			List<String> asList = Arrays.asList(split);
			String first = asList.get(0);
			String identifier = this.getIdentifier(json);
			
			boolean commandNameDoesNotMatch = false == first.trim().equals(identifier.trim());
			return commandNameDoesNotMatch;
		}

		private List<String> loadParameterNames(String name, CcpSelectUnionAll result) {
			Supplier<CcpJsonRepresentation> jsonSupplier = () -> 
			CcpOtherConstants.EMPTY_JSON
			.put(JbEntityBotCommand.Fields.commandName, name)
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

			String stepName = putParameters.getAsString(JbEntityBotCommandStepSession.Fields.stepName);
			
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
				if(k >= size2) {
					break;
				}
				String parameterValue = parameterValues.get(k++);
				json = json.put(new CcpFieldName(parameterName), parameterValue);
			}
			
			;
			
			return json;
		}

		public boolean hasExplanation(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			Optional<String> findFirst = this.explanations.stream().filter(x -> x.getAsString(Fields.language).equals(language))
			.map(x -> x.getAsString(JbEntityBotExplanation.Fields.language))
			.findFirst();
			
			boolean response = findFirst.isPresent();
			return response;
		}

		public String getExplanation(CcpJsonRepresentation json) {
			String language = json.getAsString(Fields.language);
			Optional<String> findFirst = this.explanations.stream().filter(x -> x.getAsString(Fields.language).equals(language))
			.map(x -> x.getAsString(JbEntityBotExplanation.Fields.language))
			.findFirst();
			
			String response = findFirst.orElse("");
			return response;
		}

		public String getIdentifier(CcpJsonRepresentation json) {
			
			String language = json.getAsString(Fields.language);
			
			String response = this.names.stream().filter(x -> x.getAsString(Fields.language).equals(language))
			.map(x -> "/" + x.getAsString(JbEntityBotCommand.Fields.commandName))
			.findFirst().orElseGet(() -> "/" + this.name);
			
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
		
		private void putSession(CcpJsonRepresentation json) {
			Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);
			this.sessions.put(chatId, json);
		}
		
		private void removeSession(CcpJsonRepresentation json) {
			Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);
			this.sessions.remove(chatId, json);
		}

		private CcpJsonRepresentation getSession(CcpJsonRepresentation json) {
			Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);
			CcpJsonRepresentation session = this.sessions.get(chatId);
			return session;
		}

		private boolean hasSession(CcpJsonRepresentation json) {
			Long chatId = json.getAsLongNumber(JbEntityBotCommandStepSession.Fields.chatId);
			boolean session = this.sessions.containsKey(chatId);
			return session;
		}
	}
	
	private static class BotCommandStep implements JbBotBusiness{

		private final String name;
		private final String nextStep;
		private final CcpBusiness engine;
		private final Map<Integer, String> stepFlow;
		private final List<CcpJsonRepresentation> endMessages;
		private final List<CcpJsonRepresentation> explanations;
		private final List<CcpJsonRepresentation> startMessages;
		private final Map<Integer, List<CcpJsonRepresentation>> flowMessage;
		
		private BotCommandStep(String name, CcpSelectUnionAll result) {
			this(name, loadEngine(name, result), result);
		}

		private static CcpBusiness loadEngine(String name, CcpSelectUnionAll result) {
			
			String engineName = loadFieldValue(name, result, JbEntityBotCommandStep.Fields.engine);
			
			boolean hasNoEngine = engineName.trim().isEmpty();
			
			if(hasNoEngine) {
				return CcpOtherConstants.DO_NOTHING;
			}
			CcpStringDecorator ccpStringDecorator = new CcpStringDecorator(engineName);
			CcpReflectionConstructorDecorator reflection = ccpStringDecorator.reflection();
			CcpBusiness newInstance = reflection.newInstance();
			return newInstance;
		}

		private BotCommandStep(String name, CcpBusiness engine, CcpSelectUnionAll result) {

			this.name = name;
			this.engine = engine;
			this.stepFlow = this.loadStepFlow(name, result);
			this.flowMessage = this.loadFlowMessage(name, result);
			this.endMessages = result.getEntityRows(JbEntityBotCommandStepEndMessage.ENTITY);
			this.nextStep = loadFieldValue(name, result, JbEntityBotCommandStep.Fields.nextStep);
			this.startMessages = result.getEntityRows(JbEntityBotCommandStepStartMessage.ENTITY);
			this.explanations = this.loadLabelsWithLanguages(name, result, JbEntityBotCommandStepExplanation.ENTITY, JbEntityBotCommandStepExplanation.Fields.stepName, JbEntityBotCommandStepExplanation.Fields.language, JbEntityBotCommandStepExplanation.Fields.message);
		}

		private static String loadFieldValue(String name, CcpSelectUnionAll result, CcpJsonFieldName field) {
			CcpJsonRepresentation entityRow = getEntityRow(name, result);
			String nextStep = entityRow.getAsString(field);
			return nextStep;
		}

		private CcpJsonRepresentation saveSession(CcpJsonRepresentation json, String nextStep) {
			
			CcpJsonRepresentation newJson = json.put(JbEntityBotCommandStepSession.Fields.stepName, nextStep);
			
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
				Integer status = json.getAsIntegerNumber(Fields.status);
				String stepName = json.getAsString(JbEntityBotCommandStep.Fields.stepName);
				stepFlow.put(status, stepName);
			}
			return stepFlow;
		}

		private static CcpJsonRepresentation getEntityRow(String name, CcpSelectUnionAll result) {
			CcpJsonRepresentation parametersToSearch = CcpOtherConstants.EMPTY_JSON.put(JbEntityBotCommandStep.Fields.stepName, name);
			Supplier<CcpJsonRepresentation> jsonSupplier = parametersToSearch.getJsonSupplier();
			CcpJsonRepresentation entityRow = JbEntityBotCommandStep.ENTITY.getRecordFromUnionAll(result, jsonSupplier);
			return entityRow;
		}
		
		private Map<Integer, List<CcpJsonRepresentation>> loadFlowMessage(String stepName, CcpSelectUnionAll resultFromSearchAllSteps) {
			List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JbEntityBotCommandStepFlowMessage.ENTITY);
			List<CcpJsonRepresentation> collect = entityRows.stream().filter(x -> x.getAsString(JbEntityBotCommandStepFlowMessage.Fields.stepName).equals(stepName)).collect(Collectors.toList());
			Set<Integer> allStatus = new ArrayList<>(collect).stream().map(x -> x.getAsIntegerNumber(JbEntityBotCommandStepFlowMessage.Fields.status))
			.collect(Collectors.toSet());

			var response = new HashMap<Integer, List<CcpJsonRepresentation>>();
			
			for (Integer status : allStatus) {
				List<CcpJsonRepresentation> filtered = new ArrayList<>(collect).stream().filter(x -> x.getAsIntegerNumber(JbEntityBotCommandStepFlowMessage.Fields.status).equals(status)).collect(Collectors.toList());
				response.put(status, filtered);
			}
			
			return response;
		}

		public String toString() {
			return this.name;
		}

		@SuppressWarnings("unchecked")
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			
			CcpJsonRepresentation ummutableFields = json.getJsonPiece(JbEntityBotCommandStepSession.Fields.botName, JbEntityBotCommandStepSession.Fields.chatId, JbEntityBotCommandStepSession.Fields.commandName);
			Bot bot = this.getBot(json);
			try {
				CcpJsonRepresentation sendMessageResult = bot.sendMessage(json, this.startMessages);
				boolean hasStartMessage = sendMessageResult.containsAllFields(Fields.replyTo);
			
				if(hasStartMessage) {
					Long replyTo = sendMessageResult.getAsLongNumber(Fields.replyTo);
					json = json.put(Fields.replyTo, replyTo);
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
				
				List<CcpJsonRepresentation> messages = this.flowMessage.get(e.status.asNumber());

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
	
	private static enum JsonConditions implements Predicate<CcpJsonRepresentation>{
		IfFieldExists{

			public boolean test(CcpJsonRepresentation json) {
				boolean containsAllFields = json.containsAllFields(jsonFieldName);
				return containsAllFields;
			}
		},
		thisFieldIsValidJson{

			public boolean test(CcpJsonRepresentation json) {
				CcpStringDecorator asStringDecorator = json.getAsStringDecorator(jsonFieldName);
				boolean innerJson = asStringDecorator.isInnerJson();
				return innerJson;
			}
		}
		;
		static final CcpJsonFieldName jsonFieldName = JbEntityBotCommandStepSession.Fields.json;

	}
	
	@SuppressWarnings("unchecked")
	private static enum JsonProducers implements CcpBusiness{
		sessionValuesProducer{

			public CcpJsonRepresentation apply(CcpJsonRepresentation newJson) {
				CcpJsonFieldName[] sessionFields = JbEntityBotCommandStepSession.Fields.values();
				
				CcpJsonRepresentation onlySessionValues = newJson.getJsonPiece(sessionFields);
				
				CcpJsonRepresentation handledJson = onlySessionValues.getTransformedJsonWhenAllConditionsMatch(handleInnerJson, CcpOtherConstants.RETURNS_EMPTY_JSON, JsonConditions.IfFieldExists);

				CcpJsonRepresentation onlyNoSessionValues = newJson.removeFields(sessionFields);
				CcpJsonRepresentation completedJson = handledJson.mergeWithAnotherJson(onlyNoSessionValues);
				
				CcpJsonRepresentation sessionValuesToSave = onlySessionValues.put(jsonFieldName, completedJson);
				
				return sessionValuesToSave;
			}
		},
		handleInnerJson{

			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				CcpJsonRepresentation transformedJsonWhenAllConditionsMatch = json.getTransformedJsonWhenAllConditionsMatch(getInnerJson, createInnerJson, JsonConditions.thisFieldIsValidJson);
				return transformedJsonWhenAllConditionsMatch;
			}
		},
		createInnerJson{

			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(jsonFieldName, json.get(jsonFieldName));
				return put;
			}
		},
		getInnerJson{

			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				CcpJsonRepresentation innerJson = json.getInnerJson(jsonFieldName);
				return innerJson;
			}
		},
		putCommandNameWhenHasNoSession{

			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				
				Collection<BotCommand> allCommands = JbBotEngine.INSTANCE.allCommands.values();
				
				for (BotCommand command : allCommands) {
					
					boolean commandNameDoesNotMatch = command.commandNameDoesNotMatch(json);
					
					if(commandNameDoesNotMatch) {
						continue;
					}
					
					boolean invisibleCommand = false == command.isVisible(json);
					
					if(invisibleCommand) {
						continue;
					}
					
					CcpJsonRepresentation priorityCommand = command.getCommandJson(json);
					return priorityCommand;
				}
				
				CcpJsonRepresentation putSameValueInManyFields = json.putSameValueInManyFields(JbDefaultBotCommandStep.showAllCommands, JbEntityBotCommandStepSession.Fields.commandName,JbEntityBotCommandStepSession.Fields.stepName);
				return putSameValueInManyFields;
			}
		},
		;
		static final CcpJsonFieldName jsonFieldName = JbEntityBotCommandStepSession.Fields.json;
	}
	
}
