package com.jb.entities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.engine.JbBotEngine.JbBotType;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBot.Fields.class)
public class JbEntityBot implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBot.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		botName, 

		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		commandName
		;
	}
	
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {

		List<String> commandName = Arrays.asList(JbSupportBotCommands.values()).stream().map(x -> x.name()).collect(Collectors.toList());
		
		CcpJsonRepresentation supportBot = CcpOtherConstants.EMPTY_JSON
		.put(Fields.botName, JbBotType.support.name())
		.put(Fields.commandName, commandName);
		
		
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,supportBot
				);
		return createBulkItems;
	}
	
	
}
