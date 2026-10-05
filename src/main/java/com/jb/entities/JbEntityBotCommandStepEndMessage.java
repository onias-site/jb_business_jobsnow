package com.jb.entities;

import java.util.ArrayList;
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
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.command.allowed.JbSupportAllowCommandToUserFields;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsSteps;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyFields;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchySteps;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.entities.decorators.annotations.JnEntityVersionable;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionablePurgeBuilder;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;

/**
 * A message sent after the engine of a step runs, by language; its text may refer to the values the engine produced.
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code jb_bot_command_step_end_message}</li>
 * <li>records cached for 3600 seconds</li>
 * <li>versionable: every write keeps the previous state in {@code jn_versionable}</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),})
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandStepEndMessage.Fields.class)
public class JbEntityBotCommandStepEndMessage implements CcpEntityConfigurator {

	/** The entity {@code jb_bot_command_step_end_message}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandStepEndMessage.class).entityInstance;
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code stepName} field: part of the primary key, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		stepName, 
		/** The {@code language} field: part of the primary key, text. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		language, 
		/** The {@code message} field: required, validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		message,
		
		/** The {@code instantMessageType} field: required, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		instantMessageType,

		//ATTENTION PODE SER DE IDIOMA DIFERENTE
		/** The {@code caption} field: text, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpJsonFieldTypeString
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		caption,
		
		/** The {@code contentType} field: text, validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonFieldTypeString
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		contentType, 
		
		/** The {@code fileName} field: text, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpJsonFieldTypeString
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		fileName

		;
	}

	/**
	 * The steps of the {@code fixSkillHierarchy} and {@code pendingTickets} commands end by sending the operator
	 * the text their engine left in {@code botReply} (the request, the next item, the next ticket, and so on),
	 * already written in the language of the session.
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		String botReply = "{" + JbSupportSkillFixHierarchyFields.botReply + "}";
		List<String> stepNames = Arrays.asList(
				JbSupportBotCommands.fixSkillHierarchy.name(),
				JbSupportSkillFixHierarchySteps.fixSkillHierarchyChooseMode.name(),
				JbSupportSkillFixHierarchySteps.fixSkillHierarchyDecideItem.name(),
				JbSupportSkillFixHierarchySteps.fixSkillHierarchyConfirmIgnore.name(),
				JbSupportBotCommands.pendingTickets.name(),
				JbSupportPendingTicketsSteps.pendingTicketsChoose.name());
		JnLanguage[] languages = JnLanguage.values();
		List<CcpJsonRepresentation> endMessages = new ArrayList<>();

		for (String stepName : stepNames) {
			for (JnLanguage language : languages) {
				CcpJsonRepresentation endMessageWithStepName = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.stepName, stepName);
				CcpJsonRepresentation endMessageWithLanguage = endMessageWithStepName.put(JnJsonCommonsFields.language, language);
				CcpJsonRepresentation endMessageWithMessage = endMessageWithLanguage.put(JnJsonCommonsFields.message, botReply);
				CcpJsonRepresentation endMessage = endMessageWithMessage.put(JnJsonInstantMessengerFields.instantMessageType, JnInstantMessageType.text);
				endMessages.add(endMessage);
			}
		}

		endMessages.addAll(this.getAllowCommandToUserEndMessages());

		CcpJsonRepresentation[] endMessagesArray = endMessages.toArray(new CcpJsonRepresentation[endMessages.size()]);
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(ENTITY, endMessagesArray);
		return createBulkItems;
	}

	/**
	 * The {@code allowCommandToUser} command ends telling the operator that the requests of the user for the
	 * command reach the support again.
	 */
	private List<CcpJsonRepresentation> getAllowCommandToUserEndMessages() {
		String stepName = JbSupportBotCommands.allowCommandToUser.name();
		String email = "{" + JnJsonCommonsFields.email + "}";
		String command = "{" + JbSupportAllowCommandToUserFields.command + "}";

		String portugueseMessage = "O usuário " + email + " não é mais ignorado no comando " + command + ": as próximas solicitações dele voltarão a chegar ao suporte.";
		String englishMessage = "The user " + email + " is no longer ignored in the command " + command + ": their next requests will reach the support again.";
		String spanishMessage = "El usuario " + email + " ya no es ignorado en el comando " + command + ": sus próximas solicitudes volverán a llegar al soporte.";

		List<CcpJsonRepresentation> endMessages = new ArrayList<>();
		endMessages.add(this.getEndMessage(stepName, JnLanguage.portuguese, portugueseMessage));
		endMessages.add(this.getEndMessage(stepName, JnLanguage.english, englishMessage));
		endMessages.add(this.getEndMessage(stepName, JnLanguage.spanish, spanishMessage));
		return endMessages;
	}

	/**
	 * Builds a text end message of the step in the language.
	 * @param stepName the step
	 * @param language the language
	 * @param message the text
	 * @return the record
	 */
	private CcpJsonRepresentation getEndMessage(String stepName, JnLanguage language, String message) {
		CcpJsonRepresentation endMessageWithStepName = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.stepName, stepName);
		CcpJsonRepresentation endMessageWithLanguage = endMessageWithStepName.put(JnJsonCommonsFields.language, language);
		CcpJsonRepresentation endMessageWithMessage = endMessageWithLanguage.put(JnJsonCommonsFields.message, message);
		CcpJsonRepresentation endMessage = endMessageWithMessage.put(JnJsonInstantMessengerFields.instantMessageType, JnInstantMessageType.text);
		return endMessage;
	}
}
