package com.jb.business.bots.engine;

import java.util.Collection;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.jb.entities.JbEntityBotCommandStepSession;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;

@SuppressWarnings("unchecked")
enum JsonProducers implements CcpBusiness{
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
			var get = json.get(jsonFieldName);
			CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(jsonFieldName, get);
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
				boolean visible = command.isVisible(json);

				boolean invisibleCommand = false == visible;
				
				if(invisibleCommand) {
					continue;
				}
				
				CcpJsonRepresentation priorityCommand = command.getCommandJson(json);
				return priorityCommand;
			}
			
			CcpJsonRepresentation putSameValueInManyFields = json.putSameValueInManyFields(JbDefaultBotCommandStep.showAllCommands, JnJsonInstantMessengerFields.commandName,JnJsonInstantMessengerFields.stepName);
			return putSameValueInManyFields;
		}
	},
	;
	static final CcpJsonFieldName jsonFieldName = JnJsonCommonsFields.json;
}
