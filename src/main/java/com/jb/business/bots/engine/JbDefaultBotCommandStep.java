package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jb.entities.JbEntityBotCommandStepSession;

import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;

/**
 * Commands every bot has, besides the configured ones. Each one is also its own single step, and typing it always runs
 * at once ({@link #hasPriority}).
 */
public enum JbDefaultBotCommandStep implements JbBotBusiness{
	
		/** Ends the session of the chat. */
		removeSession{
			/**
			 * Deletes the session from the database and from memory.
			 * @param json the session
			 * @return the same JSON
			 */
			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				JbEntityBotCommandStepSession.ENTITY.delete(json);
				BotCommand loadedCommand = this.getLoadedCommand(json);
				loadedCommand.removeSession(json);
				return json;
			}

			/**
			 * Never listed nor typed: it is an inner step the engine runs at the end of a command. Until 2026-10-06
			 * {@code /showAllCommands} listed it as a command.
			 * @param json the session
			 * @return {@code false}
			 */
			public boolean isVisible(CcpJsonRepresentation json) {
				return false;
			}
		},
		/** Answers with the chat id. */
		chatId{
			/**
			 * Sends the chat id.
			 * @param json the session
			 * @return the result of the sending
			 */
			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
				String chatIdText = "" + chatId;
				json = super.sendMessage(json, chatIdText);
				return json;
			}
		},
		/** Answers with the commands visible to the user. */
		showAllCommands{
			/**
			 * Sends the identifiers of the visible commands, separated by commas.
			 * @param json the session
			 * @return the result of the sending
			 */
			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				
				Bot bot = this.getBot(json);
				List<JbBotBusiness> allCommands = bot.getAllCommands(json);
				List<String> collect = new ArrayList<String>();
				for (JbBotBusiness command : allCommands) {
					boolean visible = command.isVisible(json);
					boolean isInvisibleCommand = false == visible;
					if(isInvisibleCommand) {
						continue;
					}
					
					String identifier = command.getIdentifier(json);
					collect.add(identifier);
				}
				// until 2026-10-06 the list went through toString() and then had each "," replaced by ", ", which left
				// two spaces between the commands
				String listedCommands = String.join(", ", collect);
				CcpJsonRepresentation sendMessage = super.sendMessage(json, listedCommands);
				return sendMessage;
			}
		},
		/** Answers with the explanation of the bot. */
		explainThisBot{
			/**
			 * Sends the explanation of the bot.
			 * @param json the session
			 * @return the result of the sending
			 */
			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				var bot = this.getBot(json);
				String explanation = bot.getExplanation(json);
				CcpJsonRepresentation sendMessage = super.sendMessage(json, explanation);
				return sendMessage;
			}
			
			/**
			 * Visible when the bot has an explanation in the language of the session.
			 * @param json the session
			 * @return {@code true} when visible
			 */
			public boolean isVisible(CcpJsonRepresentation json) {
				Bot bot = this.getBot(json);
				boolean hasExplanation = bot.hasExplanation(json);
				return hasExplanation;
			}
		},
		/**
		 * Answers with the explanation of the command in progress in the chat. Until 2026-10-06 it could never run: the
		 * message just typed carries no command, so it was invisible, and even when it ran the command of the session was
		 * already itself; now the command in progress is read from the session saved in the database.
		 */
		explainThisCommand{
			/**
			 * Visible when the chat has a command in progress with an explanation in the language of its session.
			 * @param json the message or the session
			 * @return {@code true} when visible
			 */
			public boolean isVisible(CcpJsonRepresentation json) {
				CcpJsonRepresentation commandInProgress = this.getCommandInProgress(json);
				boolean noCommandInProgress = false == commandInProgress.containsAllFields(JnJsonInstantMessengerFields.commandName);
				if(noCommandInProgress) {
					return false;
				}

				JbBotBusiness command = this.getLoadedCommand(commandInProgress);
				boolean hasExplanation = command.hasExplanation(commandInProgress);
				return hasExplanation;
			}

			/**
			 * Sends the explanation of the command in progress.
			 * @param json the session
			 * @return the result of the sending
			 */
			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				CcpJsonRepresentation commandInProgress = this.getCommandInProgress(json);
				JbBotBusiness command = this.getLoadedCommand(commandInProgress);
				String explanation = command.getExplanation(commandInProgress);

				CcpJsonRepresentation sendMessage = super.sendMessage(json, explanation);
				return sendMessage;
			}

			/**
			 * Returns the JSON with the command in progress and the language of the chat: the one of the JSON when it
			 * already names another command, otherwise the one of the session saved in the database (the JSON alone
			 * when there is none).
			 * @param json the message or the session
			 * @return the JSON with the command in progress
			 */
			private CcpJsonRepresentation getCommandInProgress(CcpJsonRepresentation json) {
				String commandName = json.getOrDefault(JnJsonInstantMessengerFields.commandName, () -> "");
				boolean namesAnotherCommand = false == commandName.isEmpty() && false == this.name().equals(commandName);

				if(namesAnotherCommand) {
					return json;
				}

				CcpJsonRepresentation commandInProgress = this.getSavedCommand(json);
				return commandInProgress;
			}
		},
		/**
		 * Leaves the command in progress in the chat ({@code /sair} in Portuguese, {@code /exit} in English), so the
		 * operator can run another one. The pending ticket of the command left is not closed: it stays in
		 * {@code /pendingTickets}.
		 */
		exit{
			/**
			 * Visible when the chat has a command in progress, saved in the database. The command named in the JSON is
			 * not looked at: listed by {@code /showAllCommands}, the JSON names {@code showAllCommands} itself.
			 * @param json the message or the session
			 * @return {@code true} when visible
			 */
			public boolean isVisible(CcpJsonRepresentation json) {
				CcpJsonRepresentation savedCommand = this.getSavedCommand(json);
				boolean hasCommandInProgress = savedCommand.containsAllFields(JnJsonInstantMessengerFields.commandName);
				return hasCommandInProgress;
			}

			/**
			 * Ends the session of the command in progress, in the database and in memory, and tells the operator which
			 * command was left, by its name in the language of the session.
			 * @param json the session
			 * @return the result of the sending, or the JSON when there is no command in progress
			 */
			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				CcpJsonRepresentation savedCommand = this.getSavedCommand(json);
				boolean noCommandInProgress = false == savedCommand.containsAllFields(JnJsonInstantMessengerFields.commandName);

				if(noCommandInProgress) {
					return json;
				}

				JbDefaultBotCommandStep.removeSession.execute(savedCommand);
				JbBotBusiness commandLeft = this.getLoadedCommand(savedCommand);
				String commandLeftIdentifier = commandLeft.getIdentifier(savedCommand);
				String languageName = savedCommand.getAsString(JnJsonCommonsFields.language);
				JnLanguage language = JnLanguage.valueOf(languageName);
				CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.commandName, commandLeftIdentifier);
				String commandExited = JbDefaultBotCommandMessages.commandExited.getMessage(language, parameters);

				CcpJsonRepresentation sendMessage = super.sendMessage(json, commandExited);
				return sendMessage;
			}
		},
		/**
		 * Sets the language the chat talks to the bot in ({@code /definirIdioma} in Portuguese): the replies of the bot and
		 * the names of its commands follow it. Typed alone, it shows the current language and the ones that can be chosen;
		 * the language is typed after the command, by its name in any of them ({@code /setLanguage english},
		 * {@code /definirIdioma inglês}). A command in progress goes on in the new language.
		 */
		setLanguage{
			/**
			 * Sets the typed language, or explains how to choose one.
			 * @param json the session, in the current language of the chat
			 * @return the result of the sending
			 */
			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
				String trimmedTypedValue = typedValue.trim();
				String[] typedWords = trimmedTypedValue.split("\\s+", 2);
				String currentLanguageName = json.getAsString(JnJsonCommonsFields.language);
				JnLanguage currentLanguage = JnLanguage.valueOf(currentLanguageName);
				boolean noLanguageTyped = typedWords.length < 2;

				if(noLanguageTyped) {
					CcpJsonRepresentation chooseLanguage = this.answerAboutTheLanguages(json, currentLanguage, JbDefaultBotCommandMessages.chooseLanguage, "");
					return chooseLanguage;
				}

				String typedLanguage = typedWords[1];
				List<JnLanguage> availableLanguages = BotChatLanguage.getAvailableLanguages();

				for (JnLanguage language : availableLanguages) {
					boolean notTheTypedLanguage = false == BotChatLanguage.names(typedLanguage, language);
					if(notTheTypedLanguage) {
						continue;
					}
					CcpJsonRepresentation languageSet = this.setTheLanguage(json, language);
					return languageSet;
				}

				CcpJsonRepresentation unknownLanguage = this.answerAboutTheLanguages(json, currentLanguage, JbDefaultBotCommandMessages.unknownLanguage, typedLanguage);
				return unknownLanguage;
			}

			/**
			 * Saves the language of the chat, moves the command in progress to it and confirms in the new language.
			 * @param json the session
			 * @param language the language chosen
			 * @return the result of the sending
			 */
			private CcpJsonRepresentation setTheLanguage(CcpJsonRepresentation json, JnLanguage language) {
				BotChatLanguage.setChatLanguage(json, language);
				this.moveTheCommandInProgressTo(json, language);
				String languageName = BotChatLanguage.getLanguageName(language, language);
				CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(JbDefaultBotCommandMessageFields.languageName, languageName);
				String languageSet = JbDefaultBotCommandMessages.languageSet.getMessage(language, parameters);
				CcpJsonRepresentation sendMessage = super.sendMessage(json, languageSet);
				return sendMessage;
			}

			/**
			 * Puts the new language in the session saved for the chat, when there is a command in progress.
			 * @param json the session
			 * @param language the language chosen
			 */
			private void moveTheCommandInProgressTo(CcpJsonRepresentation json, JnLanguage language) {
				CcpJsonRepresentation sessionKey = BotChatLanguage.getKey(json);
				boolean noCommandInProgress = false == JbEntityBotCommandStepSession.ENTITY.exists(sessionKey);

				if(noCommandInProgress) {
					return;
				}

				CcpJsonRepresentation savedSession = JbEntityBotCommandStepSession.ENTITY.getOneById(sessionKey);
				String languageName = language.name();
				CcpJsonRepresentation sessionWithKey = savedSession.mergeWithAnotherJson(sessionKey);
				CcpJsonRepresentation sessionInTheNewLanguage = sessionWithKey.put(JnJsonCommonsFields.language, languageName);
				JbEntityBotCommandStepSession.ENTITY.save(sessionInTheNewLanguage);
			}

			/**
			 * Sends a text that shows the current language, how to type this command and the languages that can be chosen.
			 * @param json the session
			 * @param currentLanguage the current language of the chat
			 * @param message the text
			 * @param typedLanguage the language typed, when there was one
			 * @return the result of the sending
			 */
			private CcpJsonRepresentation answerAboutTheLanguages(CcpJsonRepresentation json, JnLanguage currentLanguage, JbDefaultBotCommandMessages message, String typedLanguage) {
				String languageName = BotChatLanguage.getLanguageName(currentLanguage, currentLanguage);
				String availableLanguageNames = BotChatLanguage.getAvailableLanguageNames(currentLanguage);
				JbBotBusiness command = this.getLoadedCommand(json);
				String identifier = command.getIdentifier(json);
				CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON
						.put(JbDefaultBotCommandMessageFields.languageName, languageName)
						.put(JbDefaultBotCommandMessageFields.languages, availableLanguageNames)
						.put(JbDefaultBotCommandMessageFields.typedLanguage, typedLanguage)
						.put(JnJsonInstantMessengerFields.commandName, identifier);
				String answer = message.getMessage(currentLanguage, parameters);
				CcpJsonRepresentation sendMessage = super.sendMessage(json, answer);
				return sendMessage;
			}
		}
	;

	/**
	 * Returns the JSON with the command and the language of the session saved in the database for the chat, or the JSON
	 * without any command when there is none.
	 * @param json the message or the session
	 * @return the JSON with the saved command
	 */
	CcpJsonRepresentation getSavedCommand(CcpJsonRepresentation json) {
		// the session has the same key as the language of the chat: the bot and the chatId as a whole number
		CcpJsonRepresentation sessionKey = BotChatLanguage.getKey(json);
		boolean hasNoSavedSession = false == JbEntityBotCommandStepSession.ENTITY.exists(sessionKey);

		if(hasNoSavedSession) {
			CcpJsonRepresentation withoutCommand = json.removeFields(JnJsonInstantMessengerFields.commandName);
			return withoutCommand;
		}

		CcpJsonRepresentation savedSession = JbEntityBotCommandStepSession.ENTITY.getOneById(sessionKey);
		CcpJsonRepresentation sessionData = savedSession.getJsonPiece(JnJsonInstantMessengerFields.commandName, JnJsonCommonsFields.language);
		// the key goes along with the whole number chatId, so that the session can be deleted by this JSON
		CcpJsonRepresentation sessionDataWithKey = sessionData.mergeWithAnotherJson(sessionKey);
		CcpJsonRepresentation savedCommand = json.mergeWithAnotherJson(sessionDataWithKey);
		return savedCommand;
	}

	/**
	 * Builds the step of this command, with itself as the engine.
	 * @param result the search result
	 * @return the step
	 */
	BotCommandStep getBotCommandStep(CcpSelectUnionAll result) {
		String name = this.name();
		BotCommandStep response = new BotCommandStep(name, this, result);
		return response;
	}
		
	/**
	 * A default command always runs at once.
	 * @param json the session
	 * @return {@code true}
	 */
	public boolean hasPriority(CcpJsonRepresentation json) {
		return true;
	}
}
