package com.jb.entities;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.http.CcpHttpContentType;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jn.business.messages.JnBusinessSendInstantMessage;
import com.jn.entities.JnEntityLoginTokenRequestResend;
import com.jn.entities.JnEntitySystemMessage;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;

@CcpEntityCache(3600)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandStepStartMessage.Fields.class)
/**
 * Entidade que armazena a mensagem enviada ao usuário ao iniciar um passo do bot. Versionável,
 * cache de 1 hora. Dados iniciais configuram o template do passo {@code solveLoginTokenTicket}
 * e duas mensagens de sistema (alegações Resend e Unlock em português).
 */
public class JbEntityBotCommandStepStartMessage implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandStepStartMessage.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		stepName, 
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
	
	private CcpBulkItem toSystemMessage(CcpEntity entity, JnLanguage language, String message) {
		
		CcpJsonRepresentation json = CcpOtherConstants.EMPTY_JSON
		.put(JnEntitySystemMessage.Fields.language, language.name())
		.put(JnEntitySystemMessage.Fields.systemMessageName, entity)
		.put(JnEntitySystemMessage.Fields.message, message)
		;

		CcpEntityMetaData entityMetaData = JnEntitySystemMessage.ENTITY.getEntityMetaData();
		CcpBulkItem createBulkItem = entityMetaData.toCreateBulkItem(json);
		return createBulkItem;
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		
		String caption = "O usuário '{" + JnEntityLoginTokenRequestResend.Fields.email + "}' alega que {alegation}. Favor encaminhar a mensagem abaixo com o assunto {subject}:\n\n {message}";
		
		CcpJsonRepresentation endMessage = CcpOtherConstants.EMPTY_JSON
		.put(Fields.stepName, JbSupportBotCommands.solveLoginTokenTicket)
		.put(Fields.contentType, CcpHttpContentType.TEXT_HTML)
		.put(Fields.language, JnLanguage.portuguese)
		.put(Fields.instantMessageType, JnBusinessSendInstantMessage.JnInstantMessageType.file)
		.put(Fields.message, "{message}")
		.put(Fields.caption, caption)
		;
		
		CcpBulkItem resend = this.toSystemMessage(JnEntityLoginTokenRequestResend.ENTITY, JnLanguage.portuguese, "não recebeu e-mail com token");
		
		CcpBulkItem unlock = this.toSystemMessage(JnEntityLoginTokenRequestResend.ENTITY, JnLanguage.portuguese, "teve o token bloqueado");
		
		List<CcpBulkItem> createBulkItems = new ArrayList<CcpBulkItem>(CcpEntityConfigurator.super.toCreateBulkItems(ENTITY, endMessage));
		
		createBulkItems.add(resend);
		
		createBulkItems.add(unlock);

		return createBulkItems;
	}
}
