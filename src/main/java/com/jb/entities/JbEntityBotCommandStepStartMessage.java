package com.jb.entities;

import com.jn.entities.decorators.annotations.JnEntityVersionable;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.http.CcpHttpContentType;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;

@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),})
@JnEntityVersionable(JnVersionableEntity.class)
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
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		String valorMais = "O usuário '{" + JnJsonCommonsFields.email;
	
		String caption = valorMais + "}' alega que {alegation}. Favor encaminhar a mensagem abaixo com o assunto {subject}:\n\n {message}";
		CcpJsonRepresentation put3 = CcpOtherConstants.EMPTY_JSON
		.put(JnJsonInstantMessengerFields.stepName, JbSupportBotCommands.solveLoginTokenTicket);
		CcpJsonRepresentation put4 = put3
		.put(JnJsonInstantMessengerFields.contentType, CcpHttpContentType.TEXT_HTML);
		CcpJsonRepresentation put5 = put4
		.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		CcpJsonRepresentation put6 = put5
		.put(JnJsonInstantMessengerFields.instantMessageType, JnInstantMessageType.file);
		CcpJsonRepresentation put7 = put6
		.put(JnJsonInstantMessengerFields.message, "{message}");

		CcpJsonRepresentation endMessage = put7
		.put(JnJsonInstantMessengerFields.caption, caption)
		;
	  
		List<CcpBulkItem> toCreateBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(ENTITY, endMessage);

		List<CcpBulkItem> createBulkItems = new ArrayList<CcpBulkItem>(toCreateBulkItems);
		 
		return createBulkItems;
	}
}
