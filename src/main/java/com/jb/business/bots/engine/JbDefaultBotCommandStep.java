package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jb.entities.JbEntityBotCommandStepSession;

import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

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

				// the key of the session is calculated over the chatId as a whole number: read as a double (as it comes
				// from a parsed JSON) it would give another id
				Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
				CcpJsonRepresentation sessionKey = json
						.getJsonPiece(JnJsonInstantMessengerFields.botName)
						.put(JnJsonInstantMessengerFields.chatId, chatId);
				boolean hasNoSavedSession = false == JbEntityBotCommandStepSession.ENTITY.exists(sessionKey);

				if(hasNoSavedSession) {
					CcpJsonRepresentation withoutCommand = json.removeFields(JnJsonInstantMessengerFields.commandName);
					return withoutCommand;
				}

				CcpJsonRepresentation savedSession = JbEntityBotCommandStepSession.ENTITY.getOneById(sessionKey);
				CcpJsonRepresentation sessionData = savedSession.getJsonPiece(JnJsonInstantMessengerFields.commandName, JnJsonCommonsFields.language);
				CcpJsonRepresentation commandInProgress = json.mergeWithAnotherJson(sessionData);
				return commandInProgress;
			}
		}
	;
	
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
