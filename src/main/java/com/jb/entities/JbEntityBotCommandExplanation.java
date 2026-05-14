package com.jb.entities;

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
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnLanguage;

@CcpEntityCache(3600)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandExplanation.Fields.class)
public class JbEntityBotCommandExplanation implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandExplanation.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		commandName, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		language,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		message, 
		;
		
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {

		CcpJsonRepresentation data = CcpOtherConstants.EMPTY_JSON
		.put(Fields.commandName, JbSupportBotCommands.solveLoginTokenTicket.name())
		.put(Fields.language, JnLanguage.portuguese.name())
		.put(Fields.message, "Quando o usuário alega que seu token de login está bloqueado ou não foi enviado, ele abre uma solicitação que é recebida pelo time de suporte, então o operador de suporte evoca este comando para solucionar a solicitação que o usuário abriu")
		;
		
		
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,data
				);
		return createBulkItems;
	}

}
