package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jb.entities.JbEntityBotAllowedUser;
import com.jn.entities.JnEntityBotCommand;
import com.jn.entities.JnEntityBotCommandExplanation;
import com.jn.entities.JnEntityBotCommandName;
import com.jn.entities.JnEntityBotCommandStep;
import com.jn.entities.JnEntityBotCommandStepEndMessage;
import com.jn.entities.JnEntityBotCommandStepExplanation;
import com.jn.entities.JnEntityBotCommandStepStartMessage;
import com.jb.entities.JbEntityBotExplanation;
import com.jn.entities.subfields.JnNextStepFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;

/**
 * The engine of the support bots. A singleton that loads from the database the whole configuration of the bots (types,
 * commands, steps, messages, allowed users and explanations) and keeps them in memory.
 */
public class JbBotEngine {
	
	/** The single instance. */
	static final JbBotEngine INSTANCE = new JbBotEngine();

	/** The commands by name. */
	final Map<String, BotCommand> allCommands = new HashMap<>();
	
	/** The steps by name. */
	final Map<String, JbBotBusiness> allSteps;

	/** The bots by type. */
	final Map<CcpJsonFieldName, Bot> allBots;
	
	/** Singleton; use {@link #INSTANCE}. */
	private JbBotEngine() {
		
		JbBotType[] bots = JbBotType.values();
		JnLanguage[] jnLanguageValues = JnLanguage.values();
		int botsTimesLanguages = bots.length * jnLanguageValues.length;

		CcpJsonRepresentation[] parametersToSearchBots = new CcpJsonRepresentation[botsTimesLanguages];
	
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
				JbEntityBotExplanation.ENTITY
		);
		Map<CcpJsonFieldName, Bot> allBots = new HashMap<>();
		
		for (JbBotType botType : bots) {
			Bot bot = new Bot(botType, resultFromSearchBots);
			allBots.put(botType, bot);
		}
		
		this.allBots = allBots;
		
		List<CcpJsonRepresentation> list = new ArrayList<>();

		// the commands of each bot are the records of JnEntityBotCommand with its botName, seeded by every cost center
		// that adds commands to it; until 2026-10-15 they were a list in jb_bot, seeded only by jb
		for (JbBotType bot : bots) {
			List<String> commands = bot.getConfiguredCommands();
			Supplier<CcpJsonRepresentation> parameterToSearchBot = bot.getParameterToSearchBot();
			CcpJsonRepresentation botKey = parameterToSearchBot.get();

			for (String command : commands) {
				CcpJsonRepresentation commandKey = botKey.putSameValueInManyFields(command, JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName);
				for (var language : languages) {
					CcpJsonRepresentation parameterToSearchCommandsAndFirstSteps = commandKey.put(JnJsonCommonsFields.language, language);
					list.add(parameterToSearchCommandsAndFirstSteps);
				}
			}
		}

		// the default commands may have a name in the language too (exit is typed /sair in Portuguese); without
		// searching them here, they would be known only by their canonical name
		JbDefaultBotCommandStep[] defaultCommands = JbDefaultBotCommandStep.values();

		for (JbDefaultBotCommandStep defaultCommand : defaultCommands) {
			String defaultCommandName = defaultCommand.name();
			for (var language : languages) {
				CcpJsonRepresentation parameterToSearchTheDefaultCommand = CcpOtherConstants.EMPTY_JSON
						.putSameValueInManyFields(defaultCommandName, JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName)
						.put(JnJsonCommonsFields.language, language);
				list.add(parameterToSearchTheDefaultCommand);
			}
		}
		int listSize = list.size();

		
		CcpJsonRepresentation[] parametersToSearchCommandsAndFirstSteps = list.toArray(new CcpJsonRepresentation[listSize]);

		CcpSelectUnionAll resultFromSearchCommandsAndFirstSteps = crud.unionAll(parametersToSearchCommandsAndFirstSteps, JnDeleteKeysFromCache.INSTANCE, 
				JnEntityBotCommandStepExplanation.ENTITY, 
				JnEntityBotCommandExplanation.ENTITY, 
				JnEntityBotCommandStep.ENTITY, 
				JnEntityBotCommandName.ENTITY, 
				JnEntityBotCommand.ENTITY 
				);
		
		Map<String, CcpJsonRepresentation> allSteps = new HashMap<>();
		List<CcpJsonRepresentation> firstSteps = resultFromSearchCommandsAndFirstSteps.getEntityRows(JnEntityBotCommandStep.ENTITY);
		
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
			var nextSteps = firstStep.getAsJsonList(JnEntityBotCommandStep.Fields.stepFlow);
			for (CcpJsonRepresentation nextStep : nextSteps) {

				String nextStepName = nextStep.getAsString(JnNextStepFields.nextStep);
				String nextStepNameTrim = nextStepName.trim();
				boolean hasNoNextStep = nextStepNameTrim.isEmpty();

				if(hasNoNextStep) {
					continue;
				}

				for (var language : languages) {
					CcpJsonRepresentation putSameValueInManyFields = CcpOtherConstants.EMPTY_JSON
							.putSameValueInManyFields(nextStepNameTrim, JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName)
							;
					CcpJsonRepresentation put = putSameValueInManyFields
							.put(JnJsonCommonsFields.language, language);
					allSteps.put(nextStepNameTrim, put);
				}
			}
			CcpJsonRepresentation duplicateValueFromField = firstStep.duplicateValueFromField(JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName);
			String commandName = duplicateValueFromField.getAsString(JnJsonInstantMessengerFields.commandName);
			BotCommand botCommand = new BotCommand(commandName, resultFromSearchCommandsAndFirstSteps);
			this.allCommands.put(commandName, botCommand);
		}

		this.addStepsReachableFromTheKnownOnes(crud, allSteps, languages);

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
		int stepsTimesLanguages = parametersToSearchStepsSize * jnLanguageValues2.length;
		CcpJsonRepresentation[] parametersToSearchAllSteps = new CcpJsonRepresentation[stepsTimesLanguages];
		k = 0;
		
		for (CcpJsonRepresentation step : parametersToSearchSteps) {
			for (var language : languages) {
				CcpJsonRepresentation parameterToSearchAllSteps = step.put(JnJsonCommonsFields.language, language);
				parametersToSearchAllSteps[k++] = parameterToSearchAllSteps;
			}
		}
	
		CcpSelectUnionAll resultFromSearchAllSteps = crud.unionAll(parametersToSearchAllSteps, JnDeleteKeysFromCache.INSTANCE, 
				JnEntityBotCommandStepStartMessage.ENTITY, 
				JnEntityBotCommandStepEndMessage.ENTITY,
				JnEntityBotCommandExplanation.ENTITY, 
				JnEntityBotCommandStep.ENTITY 
				);
		
		Map<String, JbBotBusiness> stepsMap = new HashMap<>(); 
		
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(JnEntityBotCommandStep.ENTITY);
		
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
	
	/**
	 * Adds to {@code allSteps} every step reachable from the ones already there, through {@code nextStep}
	 * or through the {@code stepFlow} items, at any depth. Up to 2026-09-28 only the first step of each
	 * command and the {@code stepFlow} targets of that first step were loaded, so a command whose second
	 * step came from {@code nextStep} (or whose third step came from the second one) lost its session: the
	 * step was not in {@code allSteps} and the chain ended after the first step.
	 */
	private void addStepsReachableFromTheKnownOnes(CcpCrud crud, Map<String, CcpJsonRepresentation> allSteps, JnLanguage[] languages) {

		Set<String> stepsToVisit = new HashSet<>(allSteps.keySet());

		while(false == stepsToVisit.isEmpty()) {

			Stream<String> stepsToVisitStream = stepsToVisit.stream();
			Stream<CcpJsonRepresentation> parametersStream = stepsToVisitStream.map(stepName -> CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.stepName, stepName));
			CcpJsonRepresentation[] parametersToSearchSteps = parametersStream.toArray(CcpJsonRepresentation[]::new);
			CcpSelectUnionAll foundSteps = crud.unionAll(parametersToSearchSteps, JnDeleteKeysFromCache.INSTANCE, JnEntityBotCommandStep.ENTITY);
			List<CcpJsonRepresentation> stepRows = foundSteps.getEntityRows(JnEntityBotCommandStep.ENTITY);

			Set<String> reachedSteps = new HashSet<>();

			for (CcpJsonRepresentation stepRow : stepRows) {
				String nextStep = stepRow.getAsString(JnEntityBotCommandStep.Fields.nextStep);
				reachedSteps.add(nextStep);

				List<CcpJsonRepresentation> stepFlow = stepRow.getAsJsonList(JnEntityBotCommandStep.Fields.stepFlow);

				for (CcpJsonRepresentation flow : stepFlow) {
					String flowNextStep = flow.getAsString(JnNextStepFields.nextStep);
					reachedSteps.add(flowNextStep);
				}
			}

			Set<String> newSteps = new HashSet<>();

			for (String reachedStep : reachedSteps) {
				String reachedStepTrim = reachedStep.trim();
				boolean isBlank = reachedStepTrim.isEmpty();
				boolean alreadyKnown = allSteps.containsKey(reachedStepTrim);

				if(isBlank || alreadyKnown) {
					continue;
				}

				for (var language : languages) {
					CcpJsonRepresentation stepWithNames = CcpOtherConstants.EMPTY_JSON
							.putSameValueInManyFields(reachedStepTrim, JnJsonInstantMessengerFields.stepName, JnJsonInstantMessengerFields.commandName);
					CcpJsonRepresentation stepWithLanguage = stepWithNames.put(JnJsonCommonsFields.language, language);
					allSteps.put(reachedStepTrim, stepWithLanguage);
				}
				newSteps.add(reachedStepTrim);
			}

			stepsToVisit = newSteps;
		}
	}

	/** Fields used by the engine. */
	static enum Fields implements CcpJsonFieldName{
		/** The {@code bots} field. */
		bots,
		/** The {@code replyTo} field. */
		replyTo,
		/** The {@code commandParameters} field. */
		commandParameters,
		/** The {@code message_id} field. */
		message_id
	}
	

	

	

	

	

	
}
