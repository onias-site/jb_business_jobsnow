package com.jb.business.bots.engine;

import java.util.List;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.json.fields.validation.JnJsonCommonsFields;

import com.jn.utils.JnSystemProperties;
import java.util.stream.Stream;
import com.ccp.decorators.CcpStringDecorator;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/** Common behavior of the bot engine parts (bots, commands and steps). */
interface JbBotBusiness extends CcpBusiness{
	/**
	 * Tells whether the part is visible to the user.
	 * @param json the session
	 * @return {@code true} by default
	 */
	default boolean isVisible(CcpJsonRepresentation json) {
		return true;
	} 
	
	/**
	 * Tells whether typing the command must run at once, without loading the session.
	 * @param json the session
	 * @return {@code false} by default
	 */
	default boolean hasPriority(CcpJsonRepresentation json) {
		return false;
	} 
	
	/**
	 * Returns the bot named in the session.
	 * @param json the session
	 * @return the bot
	 */
	default Bot getBot(CcpJsonRepresentation json) {
		String name = json.getAsString(JnJsonInstantMessengerFields.botName);
		JbBotType botType = JbBotType.valueOf(name);
		var response = JbBotEngine.INSTANCE.allBots.get(botType);
		return response;
	} 

	/**
	 * Returns the command named in the session.
	 * @param json the session
	 * @return the command
	 */
	default BotCommand getLoadedCommand(CcpJsonRepresentation json) {
		Bot bot = this.getBot(json);
		BotCommand response = bot.getCommand(json);
		return response;
	}
	
	/**
	 * Reads the rows of the entity whose filter field has the given value.
	 * @param filterValue the value
	 * @param resultFromSearchAllSteps the search result
	 * @param entity the entity
	 * @param filterField the filter field
	 * @return the rows
	 */
	default List<CcpJsonRepresentation> loadLabelsWithLanguages(String filterValue, CcpSelectUnionAll resultFromSearchAllSteps, CcpEntity entity, CcpJsonFieldName filterField) {
		List<CcpJsonRepresentation> entityRows = resultFromSearchAllSteps.getEntityRows(entity);
		Stream<CcpJsonRepresentation> stream = entityRows.stream();
		var filter = stream.filter(x -> x.getAsString(filterField).equals(filterValue));
		List<CcpJsonRepresentation> response = filter.collect(Collectors.toList());
		return response;
	}
	
	/**
	 * Returns the explanation of the part.
	 * @param json the session
	 * @return an empty text by default
	 */
	default String getExplanation(CcpJsonRepresentation json) {
		return "";
	}
	
	/**
	 * Tells whether the part has an explanation.
	 * @param json the session
	 * @return {@code false} by default
	 */
	default boolean hasExplanation(CcpJsonRepresentation json) {
		return false;
	}
	
	/**
	 * Returns how the part is typed.
	 * @param json the session
	 * @return the name by default
	 */
	default String getIdentifier(CcpJsonRepresentation json) {
		String name = this.name();
		return name;
	}
	
	/**
	 * Sends a text message, with the bot token.
	 * @param json the session
	 * @param message the text
	 * @return the session merged with the answer of the provider
	 */
	default CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, String message) {
		
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.message, message);
		CcpJsonRepresentation putToken = this.putToken(json);
		CcpJsonRepresentation sendMessage = JnInstantMessageType.text.sendMessage(putToken, put);
		return sendMessage;
	}

	/**
	 * Puts the token of the bot named in the session.
	 * @param json the session
	 * @return the session with {@code botToken}
	 */
	default CcpJsonRepresentation putToken(CcpJsonRepresentation json) {
		CcpStringDecorator asStringDecorator = json.getAsStringDecorator(JnJsonInstantMessengerFields.botName);
		CcpJsonFieldName botName = asStringDecorator.jsonFieldName();
		String botToken =  JnSystemProperties.INSTANCE.getSystemInnerProperty(JbBotEngine.Fields.bots, botName);
		
		
		CcpJsonRepresentation putToken = json.put(JnJsonInstantMessengerFields.botToken, botToken);
		return putToken;
	}
}
