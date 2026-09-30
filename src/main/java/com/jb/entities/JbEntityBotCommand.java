package com.jb.entities;

import com.jn.entities.decorators.annotations.JnEntityVersionable;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import java.util.Arrays;
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
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.command.allowed.JbSupportAllowCommandToUserFields;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.login.token.JbSupportLoginToken;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionablePurgeBuilder;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),})
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommand.Fields.class)
/**
 * Entidade que representa um comando de bot com seus parâmetros nomeados. Versionável e com
 * cache de 1 hora. Dados iniciais registram {@code solveLoginTokenTicket} sem parâmetros.
 */
public class JbEntityBotCommand implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommand.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		commandName, 
		
		@CcpJsonFieldTypeString(allowsEmptyString = false)
		@CcpJsonFieldValidatorArray
		parameterName
		;
		
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {

		String solveLoginTokenTicketName = JbSupportBotCommands.solveLoginTokenTicket.name();
		CcpJsonRepresentation put2 = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.commandName, solveLoginTokenTicketName);
		List<?> parameters = Arrays.asList(JbSupportLoginToken.JsonFields.ticketType.name(), JnJsonCommonsFields.email.name());
		CcpJsonRepresentation data = put2.put(JbEntityBotCommand.Fields.parameterName, parameters);
		
		
		String fixSkillHierarchyName = JbSupportBotCommands.fixSkillHierarchy.name();
		CcpJsonRepresentation fixSkillHierarchyWithName = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.commandName, fixSkillHierarchyName);
		List<?> fixSkillHierarchyParameters = Arrays.asList(VisEntitySkillFixHierarchyPending.Fields.parent.name(), VisEntitySkillFixHierarchyPending.Fields.email.name());
		CcpJsonRepresentation fixSkillHierarchy = fixSkillHierarchyWithName.put(JbEntityBotCommand.Fields.parameterName, fixSkillHierarchyParameters);

		String allowCommandToUserName = JbSupportBotCommands.allowCommandToUser.name();
		CcpJsonRepresentation allowCommandToUserWithName = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.commandName, allowCommandToUserName);
		List<?> allowCommandToUserParameters = Arrays.asList(JbSupportAllowCommandToUserFields.command.name(), JnJsonCommonsFields.email.name());
		CcpJsonRepresentation allowCommandToUser = allowCommandToUserWithName.put(JbEntityBotCommand.Fields.parameterName, allowCommandToUserParameters);

		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,data
				,fixSkillHierarchy
				,allowCommandToUser
				);
		return createBulkItems;
	}

}
