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
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.login.token.JbSupportLoginToken;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionablePurgeBuilder;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillPending;

/**
 * A bot command and the names of its parameters, in the order the operator types them after the command.
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code jb_bot_command}</li>
 * <li>records cached for 3600 seconds</li>
 * <li>versionable: every write keeps the previous state in {@code jn_versionable}</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),})
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommand.Fields.class)
public class JbEntityBotCommand implements CcpEntityConfigurator {

	/** The entity {@code jb_bot_command}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommand.class).entityInstance;
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code commandName} field: part of the primary key, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		commandName, 
		
		/** The {@code parameterName} field: text, list. */
		@CcpJsonFieldTypeString(allowsEmptyString = false)
		@CcpJsonFieldValidatorArray
		parameterName
		;
		
	}
	
	/**
	 * Seeds the parameters of the support commands: {@code solveLoginTokenTicket <ticketType> <email>},
	 * {@code fixSkillHierarchy <type> <email> <parent>}, {@code allowCommandToUser <email>} and
	 * {@code reviewSkillSuggestion <email> <skill>}.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {

		String solveLoginTokenTicketName = JbSupportBotCommands.solveLoginTokenTicket.name();
		CcpJsonRepresentation put2 = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.commandName, solveLoginTokenTicketName);
		List<?> parameters = Arrays.asList(JbSupportLoginToken.JsonFields.ticketType.name(), JnJsonCommonsFields.email.name());
		CcpJsonRepresentation data = put2.put(JbEntityBotCommand.Fields.parameterName, parameters);
		
		
		String fixSkillHierarchyName = JbSupportBotCommands.fixSkillHierarchy.name();
		CcpJsonRepresentation fixSkillHierarchyWithName = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.commandName, fixSkillHierarchyName);
		// /fixSkillHierarchy <type> <email> <parent>: the operator reviews one type (add or remove) at a time; the parent goes
		// last because it may have spaces (until 2026-10-08 it went first and FRONT END was read as parent FRONT, type END)
		List<?> fixSkillHierarchyParameters = Arrays.asList(VisEntitySkillFixHierarchyPending.Fields.type.name(), VisEntitySkillFixHierarchyPending.Fields.email.name(), VisEntitySkillFixHierarchyPending.Fields.parent.name());
		CcpJsonRepresentation fixSkillHierarchy = fixSkillHierarchyWithName.put(JbEntityBotCommand.Fields.parameterName, fixSkillHierarchyParameters);

		String allowCommandToUserName = JbSupportBotCommands.allowCommandToUser.name();
		CcpJsonRepresentation allowCommandToUserWithName = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.commandName, allowCommandToUserName);
		// /allowCommandToUser <email>: the ignoring is global, so the command is not a parameter (since 2026-10-08)
		List<?> allowCommandToUserParameters = Arrays.asList(JnJsonCommonsFields.email.name());
		CcpJsonRepresentation allowCommandToUser = allowCommandToUserWithName.put(JbEntityBotCommand.Fields.parameterName, allowCommandToUserParameters);

		String reviewSkillSuggestionName = JbSupportBotCommands.reviewSkillSuggestion.name();
		CcpJsonRepresentation reviewSkillSuggestionWithName = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.commandName, reviewSkillSuggestionName);
		// /reviewSkillSuggestion <email> <skill>: the skill goes last because it may have spaces (the last parameter takes the rest of the text)
		List<?> reviewSkillSuggestionParameters = Arrays.asList(VisEntitySkillPending.Fields.email.name(), VisEntitySkillPending.Fields.skill.name());
		CcpJsonRepresentation reviewSkillSuggestion = reviewSkillSuggestionWithName.put(JbEntityBotCommand.Fields.parameterName, reviewSkillSuggestionParameters);

		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,data
				,fixSkillHierarchy
				,allowCommandToUser
				,reviewSkillSuggestion
				);
		return createBulkItems;
	}

}
