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

interface JbBotBusiness extends CcpBusiness{
	default boolean isVisible(CcpJsonRepresentation json) {
		return true;
	}
	
	default boolean hasPriority(CcpJsonRepresentation json) {
		return false;
	}
	
	default Bot getBot(CcpJsonRepresentation json) {
		String name = json.getAsString(JnJsonInstantMessengerFields.botName);
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
		Stream<CcpJsonRepresentation> stream = entityRows.stream();
		var filter = stream.filter(x -> x.getAsString(filterField).equals(filterValue));
		List<CcpJsonRepresentation> response = filter.collect(Collectors.toList());
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
		CcpStringDecorator asStringDecorator = json.getAsStringDecorator(JnJsonInstantMessengerFields.botName);
		CcpJsonFieldName botName = asStringDecorator.jsonFieldName();
		String botToken =  JnSystemProperties.INSTANCE.getSystemInnerProperty(JbBotEngine.Fields.bots, botName);
		
		
		CcpJsonRepresentation putToken = json.put(JnJsonInstantMessengerFields.botToken, botToken);
		return putToken;
	}
}
