package com.jb.business.bots.engine;

import java.util.Collection;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.jb.entities.JbEntityBotCommandStepSession;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/** Transformations of the session JSON. */
@SuppressWarnings("unchecked")
enum JsonProducers implements CcpBusiness{
	/** Builds the record of the session: the session fields plus, in {@code json}, every other value. */
	sessionValuesProducer{

		/**
		 * Builds the record of the session.
		 * @param newJson the session
		 * @return the session record
		 */
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
	/** Reads the {@code json} field as JSON when it is one; otherwise wraps it. */
	handleInnerJson{

		/**
		 * Handles the {@code json} field.
		 * @param json the session fields
		 * @return the inner JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation transformedJsonWhenAllConditionsMatch = json.getTransformedJsonWhenAllConditionsMatch(getInnerJson, createInnerJson, JsonConditions.thisFieldIsValidJson);
			return transformedJsonWhenAllConditionsMatch;
		}
	},
	/** Wraps the value of the {@code json} field. */
	createInnerJson{

		/**
		 * Wraps the value.
		 * @param json the session fields
		 * @return a JSON with the {@code json} field only
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			var get = json.get(jsonFieldName);
			CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(jsonFieldName, get);
			return put;
		}
	},
	/** Reads the {@code json} field as JSON. */
	getInnerJson{

		/**
		 * Reads the inner JSON.
		 * @param json the session fields
		 * @return the inner JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation innerJson = json.getInnerJson(jsonFieldName);
			return innerJson;
		}
	},
	/** Without a session, starts the visible command typed by the user, or {@code showAllCommands} when none was typed. */
	putCommandNameWhenHasNoSession{

		/**
		 * Puts the command and the step.
		 * @param json the message
		 * @return the session at the start of the command
		 */
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
	/** The {@code json} field. */
	static final CcpJsonFieldName jsonFieldName = JnJsonCommonsFields.json;
}
