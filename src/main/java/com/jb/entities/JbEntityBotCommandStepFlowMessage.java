package com.jb.entities;

import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberInteger;
import com.jb.business.bots.engine.JbDefaultBotCommandStep;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.login.token.JbSupportLoginToken;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

import com.jn.utils.JnLanguage;

@CcpEntityCache(3600)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandStepFlowMessage.Fields.class)
/**
 * Entidade que armazena mensagens associadas a status específicos de fluxo de erro
 * ({@code CcpErrorFlowDisturb}). Chave primária: {@code stepName} + {@code status} + {@code language}.
 * Versionável, cache de 1 hora.
 */
public class JbEntityBotCommandStepFlowMessage implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandStepFlowMessage.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		stepName, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeNumberInteger
		status,
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		language, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		message, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		instantMessageType,

		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		caption,
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		contentType,
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		fileName

		;
		
		
	}
		public List<CcpBulkItem> getFirstRecordsToInsert() {
			String showAllCommandsName = JbDefaultBotCommandStep.showAllCommands.name();
			CcpJsonRepresentation put8 = CcpOtherConstants.EMPTY_JSON
			.put(JnJsonInstantMessengerFields.stepName, showAllCommandsName);
			String portugueseName = JnLanguage.portuguese.name();
			CcpJsonRepresentation put9 = put8
			.put(JnJsonCommonsFields.language, portugueseName);
			CcpJsonRepresentation put10 = put9
			.put(JnJsonInstantMessengerFields.message, "O comando digitado é inválido, abaixo uma lista válida de comandos:");
			CcpJsonRepresentation put11 = put10
			.put(JnJsonCommonsFields.status, 404);
			
			CcpJsonRepresentation showAllCommands = put11
			.put(JnJsonInstantMessengerFields.instantMessageType, JnInstantMessageType.text)
			;
			CcpJsonRepresentation solveLoginTokenTicket = this.solveLoginTokenTicket();

			List<CcpBulkItem> toCreateBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(ENTITY, showAllCommands, solveLoginTokenTicket);
		
			return toCreateBulkItems;
		}
	
		protected CcpJsonRepresentation solveLoginTokenTicket() {
			String solveLoginTokenTicketName = JbSupportBotCommands.solveLoginTokenTicket.name();
			CcpJsonRepresentation put8 = CcpOtherConstants.EMPTY_JSON
			.put(JnJsonInstantMessengerFields.stepName, solveLoginTokenTicketName);
			String portugueseName = JnLanguage.portuguese.name();
			CcpJsonRepresentation put9 = put8
			.put(JnJsonCommonsFields.language, portugueseName);
			CcpJsonRepresentation put10 = put9
			.put(JnJsonInstantMessengerFields.message, "O e-mail '{" + JnJsonCommonsFields.email + "}' não possui pendência de '{"  + JbSupportLoginToken.JsonFields.ticketType + "}'");
			CcpJsonRepresentation put11 = put10
			.put(JnJsonCommonsFields.status, 404);
			
			CcpJsonRepresentation solveLoginTokenTicket = put11
			.put(JnJsonInstantMessengerFields.instantMessageType, JnInstantMessageType.text)
			;
			return solveLoginTokenTicket;
		}
}
