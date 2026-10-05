package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jb.entities.JbEntityBotCommandStepSession;

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
				String toString = collect
						.toString();
						String toStringReplace = toString
						.replace("[", "");
						String toStringReplaceReplace = toStringReplace
						.replace("]", "");

						String listedCommands = toStringReplaceReplace
						.replace(",", ", ")
						;
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
		/** Answers with the explanation of the current command. */
		explainThisCommand{
			/**
			 * Visible when there is a current command with an explanation in the language of the session.
			 * @param json the session
			 * @return {@code true} when visible
			 */
			public boolean isVisible(CcpJsonRepresentation json) {
				boolean containsAllFields = json.containsAllFields(JnJsonInstantMessengerFields.commandName);
				boolean commandLess = false == containsAllFields;
				if(commandLess) {
					return false;
				}
				
				JbBotBusiness command = this.getLoadedCommand(json);
				boolean hasExplanation = command.hasExplanation(json);
				return hasExplanation;
			}

			/**
			 * Sends the explanation of the current command.
			 * @param json the session
			 * @return the result of the sending
			 */
			public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
				JbBotBusiness command = this.getLoadedCommand(json);
				String explanation = command.getExplanation(json);
				
				CcpJsonRepresentation sendMessage = super.sendMessage(json, explanation);
				return sendMessage;
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
