package com.jb.entities;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constantes.CcpOtherConstants;
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
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jn.entities.JnEntityLoginTokenRequestResend;
import com.jn.entities.JnEntitySystemMessage;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnLanguage;

@CcpEntityCache(3600)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandStepStartMessage.Fields.class)
public class JbEntityBotCommandStepStartMessage implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandStepStartMessage.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		stepName, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		language, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		message, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		type,

		@CcpJsonFieldTypeString
		caption,
		
		@CcpJsonFieldTypeString
		contentType,
		
		@CcpJsonFieldTypeString
		fileName

		;
	}
	
	private CcpBulkItem toSystemMessage(CcpEntity entity, JnLanguage language, String message) {
		
		CcpJsonRepresentation json = CcpOtherConstants.EMPTY_JSON
		.put(JnEntitySystemMessage.Fields.language, language.name())
		.put(JnEntitySystemMessage.Fields.systemMessageName, entity.name())
		.put(JnEntitySystemMessage.Fields.message, message)
		;

		CcpEntityMetaData entityMetaData = JnEntitySystemMessage.ENTITY.getEntityMetaData();
		CcpBulkItem createBulkItem = entityMetaData.toCreateBulkItem(json);
		return createBulkItem;
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		
		String caption = "O usuário '{" + JnEntityLoginTokenRequestResend.Fields.email + "}' alega que {alegation}. Favor encaminhar a mensagem abaixo com o assunto {subject}:\n\n {message}";
		
		CcpJsonRepresentation endMessage = CcpOtherConstants.EMPTY_JSON
		.put(Fields.stepName, JbSupportBotCommands.solveLoginTokenTicket.name())
		.put(Fields.contentType, CcpHttpContentType.TEXT_HTML.name())
		.put(Fields.language, JnLanguage.portuguese.name())
		.put(Fields.type, com.jn.business.messages.JnBusinessSendInstantMessage.MessageType.file.name())
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
