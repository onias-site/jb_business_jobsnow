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
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.engine.JbBotEngine.JbBotType;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnLanguage;

@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotExplanation.Fields.class)
public class JbEntityBotExplanation implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotExplanation.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		botName, 
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
		.put(Fields.language, JnLanguage.portuguese.name())
		.put(Fields.botName, JbBotType.support.name())
		.put(Fields.message, "")
		;
		
		
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,data
				);
		return createBulkItems;
	}

}
