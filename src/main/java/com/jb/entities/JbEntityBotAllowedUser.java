package com.jb.entities;

import java.util.Arrays;
import java.util.List;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.engine.JbBotEngine.JbBotType;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotAllowedUser.Fields.class)
public class JbEntityBotAllowedUser implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotAllowedUser.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		botName, 

		@CcpJsonFieldValidatorArray
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumberUnsigned
		allowedUser
		;
	}
	
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {

		List<Long> allowedUser = Arrays.asList(751717896L);
		
		CcpJsonRepresentation data = CcpOtherConstants.EMPTY_JSON
		.put(Fields.botName, JbBotType.support.name())
		.put(Fields.allowedUser, allowedUser);
		
		
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,data
				);
		return createBulkItems;
	}
}
