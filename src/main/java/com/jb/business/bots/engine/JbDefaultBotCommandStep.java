package com.jb.business.bots.engine;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jb.entities.JbEntityBotCommandStepSession;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

public enum JbDefaultBotCommandStep implements JbBotBusiness{
	
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
				Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
				String valorMais = "" + chatId;
				json = super.sendMessage(json, valorMais);
				return json;
			}
		},
		showAllCommands{
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
				boolean containsAllFields = json.containsAllFields(JnJsonInstantMessengerFields.commandName);
				boolean commandLess = false == containsAllFields;
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
	
	BotCommandStep getBotCommandStep(CcpSelectUnionAll result) {
		String name = this.name();
		BotCommandStep response = new BotCommandStep(name, this, result);
		return response;
	}
		
	public boolean hasPriority(CcpJsonRepresentation json) {
		return true;
	}
}
