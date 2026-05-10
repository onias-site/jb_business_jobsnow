package com.jb.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberInteger;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandStepSession.Fields.class)
@CcpEntityDisposable(expurgTime = CcpEntityExpurgableOptions.daily, expurgableEntityFactory = JnDisposableEntity.class)
public class JbEntityBotCommandStepSession implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandStepSession.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{


		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeNumberInteger
		chatId,

		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		botName, 

		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		commandName, 

		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		stepName, 
		
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson
		json

		;
		
	}
}
