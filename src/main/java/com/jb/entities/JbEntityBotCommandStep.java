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
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.login.token.JbBotSolveLoginTokenTicket;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

@CcpEntityCache(3600)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandStep.Fields.class)
public class JbEntityBotCommandStep implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandStep.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		stepName, 
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson
		stepFlow,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		engine,
		@CcpJsonFieldTypeString
		nextStep,
		;
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {

		String solveLoginTokenTicket = JbSupportBotCommands.solveLoginTokenTicket.name();
		
		String engine = JbBotSolveLoginTokenTicket.class.getName();
		CcpJsonRepresentation solveLoginTokenTicketCommand = CcpOtherConstants.EMPTY_JSON
				.put(Fields.stepFlow, CcpOtherConstants.EMPTY_JSON)
				.put(Fields.stepName, solveLoginTokenTicket)
				.put(Fields.nextStep, solveLoginTokenTicket)
				.put(Fields.engine, engine)
		;
		
		
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,solveLoginTokenTicketCommand
				);
		return createBulkItems;
	}
}
