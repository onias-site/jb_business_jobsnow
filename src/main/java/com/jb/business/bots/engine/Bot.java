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
import com.jb.business.bots.login.token.JbBotSolveLoginTokenTicket.StepFields;
import com.jb.entities.JbEntityBot;
import com.jb.entities.JbEntityBotAllowedUser;
import com.jb.entities.JbEntityBotCommandStepSession;
import com.jb.entities.JbEntityBotExplanation;
import com.jn.business.messages.JnBusinessSendInstantMessage;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.utils.JnLanguage;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;

class Bot implements JbBotBusiness{
	private final JbBotType botType;
	private final boolean isRestricted;
	private final List<String> commands;
	private final Set<Long> allowedUsers;
	private final List<CcpJsonRepresentation> explanations;

	Bot(JbBotType botType, CcpSelectUnionAll resultFromSearchBots) {
		String botTypeName = botType.name();
		this.explanations = this.loadLabelsWithLanguages(botTypeName, resultFromSearchBots, JbEntityBotExplanation.ENTITY, JnJsonInstantMessengerFields.botName, JnJsonCommonsFields.language, JnJsonInstantMessengerFields.message);
		this.allowedUsers = this.loadAllowedUsers(botType, resultFromSearchBots);
		this.commands = this.loadCommands(botType, resultFromSearchBots);
		this.isRestricted = botType.isRestricted();
		this.botType = botType;
		
	}
	
	private Set<Long> loadAllowedUsers(JbBotType valueOf, CcpSelectUnionAll resultFromSearchBots) {

		Supplier<CcpJsonRepresentation> parameterToSearchBot = valueOf.getParameterToSearchBot();
		CcpJsonRepresentation recordFromUnionAll = JbEntityBotAllowedUser.ENTITY.getRecordFromUnionAll(resultFromSearchBots, parameterToSearchBot);
		List<String> asStringList = recordFromUnionAll.getAsStringList(JbEntityBotAllowedUser.Fields.allowedUser);
		Stream<String> stream = asStringList.stream();
		var streamMap = stream.map(x -> Double.valueOf(x).longValue());
		Set<Long> collect = streamMap.collect(Collectors.toSet());
		return collect;
	}

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
	
	public String toString() {
		String name = this.name();
		return name;
	}
	
	
	CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, List<CcpJsonRepresentation> messages) {
		
		String language = json.getAsString(JnJsonCommonsFields.language);
		String stepName = json.getAsString(JnJsonInstantMessengerFields.stepName);
		Stream<CcpJsonRepresentation> stream3 = messages.stream();
		var filter = stream3
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

		String type = message.getOrDefault(JnJsonInstantMessengerFields.instantMessageType, () -> JnInstantMessageType.text.name());
		
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
			boolean session2 = command.hasSession(json);

			boolean hasNoSession = false == session2;
			
			if(hasNoSession) {
				json = command.getCommandJson(json);
				break;
			}
			
			CcpJsonRepresentation session = command.getSession(json);
			CcpJsonRepresentation innerJson = session.getInnerJson(JnJsonCommonsFields.json);
			CcpJsonRepresentation mergeWithAnotherJson = innerJson.mergeWithAnotherJson(session);
			return mergeWithAnotherJson;
		}
		
		

		CcpEntityMetaData entityMetaData = JbEntityBotCommandStepSession.ENTITY.getEntityMetaData();
		
		CcpBusiness newSessionProducer = this.newSessionProducer(json);
		
		CcpJsonRepresentation savedSession = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(json, newSessionProducer);
		CcpJsonRepresentation innerJson = savedSession.getInnerJson(JnJsonCommonsFields.json);
		CcpJsonRepresentation removeFields = savedSession.removeFields(JnJsonCommonsFields.json);
		CcpJsonRepresentation handledSession = innerJson.mergeWithAnotherJson(removeFields);

		return handledSession;
	}

	public boolean isVisible(CcpJsonRepresentation json) {
		
		boolean openBot = false == this.isRestricted;
		if(openBot) {
			return true;
		}
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);

		boolean alloedUser = this.allowedUsers.contains(chatId);
		
		return alloedUser;
	}
	
	private CcpBusiness newSessionProducer(CcpJsonRepresentation json) {
		CcpBusiness newSessionProducer = jsn -> 
		json
		.mergeWithAnotherJson(json)
		.renameField(JnJsonInstantMessengerFields.message, StepFields.typedValue)
		.put(JnJsonCommonsFields.language, JnLanguage.portuguese)
		.getTransformedJson(JsonProducers.putCommandNameWhenHasNoSession)
		;
		return newSessionProducer;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation message) {
		String botTypeName2 = botType.name();
		CcpJsonRepresentation put = message
				.put(JnJsonInstantMessengerFields.botName, botTypeName2);
				CcpJsonRepresentation put2 = put
				//TODO PARAMETRIZAR ESSE TEXT
				.put(JnJsonInstantMessengerFields.instantMessageType, JnInstantMessageType.text);
				CcpJsonRepresentation json = put2
				.renameField(JbBotEngine.Fields.message_id, JnBusinessSendInstantMessage.Fields.replyTo)
				;

		CcpJsonRepresentation renameField = json.renameField(JnJsonInstantMessengerFields.message, StepFields.typedValue);
		CcpJsonRepresentation loadSession = this.loadSession(renameField);
		BotCommand botCommand = this.getCommand(loadSession);
		CcpJsonRepresentation execute = botCommand.execute(loadSession);
		return execute;
	}

	protected BotCommand getCommand(CcpJsonRepresentation json) {
		String commandName = json.getAsString(JnJsonInstantMessengerFields.commandName);
		BotCommand botCommand = JbBotEngine.INSTANCE.allCommands.get(commandName);
		return botCommand;
	} 
	
	public String name() {
		String botTypeName3 = this.botType.name();
		return botTypeName3;
	}
	
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
	
	List<JbBotBusiness> getAllCommands(CcpJsonRepresentation json){
		Stream<String> stream6 = this.commands.stream();
		var stream6Map = stream6.map(x -> this.getCommand(x));
		List<JbBotBusiness> collect = stream6Map.collect(Collectors.toList());
		return collect;
	}
	
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

	/**
	 * Exceção lançada quando o bot não possui explicação cadastrada no idioma pedido.
	 */
	@SuppressWarnings("serial")
	public static class JbErrorBotExplanationLanguageIsMissing extends RuntimeException {
		/**
		 * Monta a mensagem informando qual idioma falta e em qual bot.
		 * @param language o idioma sem explicação cadastrada
		 * @param botName o nome do bot consultado
		 */
		private JbErrorBotExplanationLanguageIsMissing(String language, String botName) {
			super("'" + language + "' is missing in the explanations of the bot '" + botName + "'");
		}
	}

	/**
	 * Exceção lançada quando se pede ao motor de bots um comando que não está registrado.
	 */
	@SuppressWarnings("serial")
	public static class JbErrorBotCommandNotFound extends RuntimeException {
		/**
		 * Monta a mensagem informando qual comando não foi encontrado.
		 * @param commandName o nome do comando procurado
		 */
		private JbErrorBotCommandNotFound(String commandName) {
			super("The command '" + commandName + "' whas not found");
		}
	}
}
