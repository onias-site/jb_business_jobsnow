package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jb.entities.JbEntityBot;
import com.jb.entities.JbEntityBotAllowedUser;
import com.jb.entities.JbEntityBotCommand;
import com.jb.entities.JbEntityBotCommandExplanation;
import com.jb.entities.JbEntityBotCommandName;
import com.jb.entities.JbEntityBotCommandStep;
import com.jb.entities.JbEntityBotCommandStepEndMessage;
import com.jb.entities.JbEntityBotCommandStepExplanation;
import com.jb.entities.JbEntityBotCommandStepFlowMessage;
import com.jb.entities.JbEntityBotCommandStepStartMessage;
import com.jb.entities.JbEntityBotExplanation;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;

import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * Motor central dos bots de suporte Telegram do jobsnow. Inicializado como singleton,
 * carrega do Elasticsearch toda a configuração dos bots (tipos, comandos, passos, mensagens,
 * usuários permitidos e explicações) e gerencia o fluxo de interação multi-passo: receber
 * texto do usuário, identificar o bot e o comando, avançar a sessão e enviar respostas.
 */
public class JbBotEngine {
	
	static final JbBotEngine INSTANCE = new JbBotEngine();

	final Map<String, BotCommand> allCommands = new HashMap<>();
	
	final Map<String, JbBotBusiness> allSteps;

	final Map<CcpJsonFieldName, Bot> allBots;
	
	private JbBotEngine() {
		
		JbBotType[] bots = JbBotType.values();
		JnLanguage[] jnLanguageValues = JnLanguage.values();
		int lengthVezes = bots.length * jnLanguageValues.length;

		CcpJsonRepresentation[] parametersToSearchBots = new CcpJsonRepresentation[lengthVezes];
	
		var languages = JnLanguage.values();
		
		int k = 0;
		for (JbBotType bot : bots) {
			for (var language : languages) {
				Supplier<CcpJsonRepresentation> parameterToSearchBot = bot.getParameterToSearchBot();
				CcpJsonRepresentation parameterToSearchBots = parameterToSearchBot.get();
				parametersToSearchBots[k++] = parameterToSearchBots
						.put(JnJsonCommonsFields.language, language)
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
			List<String> commands = recordFromUnionAll.getAsStringList(JnJsonInstantMessengerFields.commandName);
			
			String language = parameterToSearchBot.getAsString(JnJsonCommonsFields.language);
			
			for (String command : commands) {
				CcpJsonRepresentation putSameValueInManyFields = recordFromUnionAll.putSameValueInManyFields(command, JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName);

					CcpJsonRepresentation parameterToSearchCommandsAndFirstSteps = 
							putSameValueInManyFields
						.put(JnJsonCommonsFields.language, language)
						;

					list.add(parameterToSearchCommandsAndFirstSteps);
				}
		}
		int listSize = list.size();

		
		CcpJsonRepresentation[] parametersToSearchCommandsAndFirstSteps = list.toArray(new CcpJsonRepresentation[listSize]);

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
			String stepName = firstStep.getAsString(JnJsonInstantMessengerFields.stepName);
			for (var language : languages) {
				CcpJsonRepresentation duplicateValueFromField2 = firstStep
						.duplicateValueFromField(JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName);
						CcpJsonRepresentation putLanguage = duplicateValueFromField2
						.put(JnJsonCommonsFields.language, language)
						;
				allSteps.put(stepName, putLanguage);
			}
			var nextSteps = firstStep.getAsJsonList(JbEntityBotCommandStep.Fields.stepFlow);
			for (CcpJsonRepresentation nextStep : nextSteps) {
				for (var language : languages) {
					CcpJsonRepresentation duplicateValueFromField = nextStep
							.duplicateValueFromField(JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName)
							;
					CcpJsonRepresentation put = duplicateValueFromField
							.put(JnJsonCommonsFields.language, language);
					allSteps.put(stepName, put);
				}
			}
			CcpJsonRepresentation duplicateValueFromField = firstStep.duplicateValueFromField(JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName);
			String commandName = duplicateValueFromField.getAsString(JnJsonInstantMessengerFields.commandName);
			BotCommand botCommand = new BotCommand(commandName, resultFromSearchCommandsAndFirstSteps);
			this.allCommands.put(commandName, botCommand);
		}

		JbDefaultBotCommandStep[] defaultBotCommandSteps = JbDefaultBotCommandStep.values();
	
		for (JbDefaultBotCommandStep defaultBotCommandStep : defaultBotCommandSteps) {
			String commandName = defaultBotCommandStep.name();
			BotCommand botCommand = new BotCommand(commandName, resultFromSearchCommandsAndFirstSteps);
			this.allCommands.put(commandName, botCommand);

			var step = CcpOtherConstants.EMPTY_JSON.putSameValueInManyFields(commandName,
					JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName);
			allSteps.put(commandName, step);
		}
		
		Collection<CcpJsonRepresentation> parametersToSearchSteps = allSteps.values();
		int parametersToSearchStepsSize = parametersToSearchSteps.size();
		JnLanguage[] jnLanguageValues2 = JnLanguage.values();
		int parametersToSearchStepsSizeVezes = parametersToSearchStepsSize * jnLanguageValues2.length;
		CcpJsonRepresentation[] parametersToSearchAllSteps = new CcpJsonRepresentation[parametersToSearchStepsSizeVezes];
		k = 0;
		
		for (CcpJsonRepresentation step : parametersToSearchSteps) {
			for (var language : languages) {
				CcpJsonRepresentation parameterToSearchAllSteps = step.put(JnJsonCommonsFields.language, language);
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
			String stepName = entityRow.getAsString(JnJsonInstantMessengerFields.stepName);
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
	
	static enum Fields implements CcpJsonFieldName{
		bots, replyTo, commandParameters, message_id
	}
	

	

	

	

	

	
}
