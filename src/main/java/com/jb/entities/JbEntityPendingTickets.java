package com.jb.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.JnEntitySupportPendingCommand;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * The tickets (commands sent by the platform to the support operator) the operator has not solved yet, listed by {@code /pendingTickets}; a ticket leaves the list when the command that solves it finishes.
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code jb_pending_tickets}</li>
 * </ul>
 */
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityPendingTickets.Fields.class)
public class JbEntityPendingTickets implements CcpEntityConfigurator {

	/** The entity {@code jb_pending_tickets}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityPendingTickets.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code botName} field: part of the primary key, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		botName,

		/** The {@code chatId} field: part of the primary key, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		chatId,

		/** The {@code ticket} field: part of the primary key, text. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString
		ticket,

		/** The {@code timestamp} field: required, validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,
		;
	}
}
