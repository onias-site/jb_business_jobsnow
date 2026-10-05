package com.jb.entities;

import java.util.Arrays;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.jb.business.bots.engine.JbBotType;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * The chat ids allowed to use a restricted bot (the support bot).
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code jb_bot_allowed_user}</li>
 * <li>records cached for 3600 seconds</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotAllowedUser.Fields.class)
public class JbEntityBotAllowedUser implements CcpEntityConfigurator {

	/** The entity {@code jb_bot_allowed_user}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotAllowedUser.class).entityInstance;
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code botName} field: part of the primary key, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		botName, 

		/** The {@code allowedUser} field: list, required, non-negative integer. */
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumberUnsigned
		allowedUser
		;
	}
	
	
	/**
	 * Seeds the chat id allowed to use the support bot.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {

		List<Long> allowedUser = Arrays.asList(751717896L);
		String supportName = JbBotType.support.name();
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
		.put(JnJsonInstantMessengerFields.botName, supportName);

		CcpJsonRepresentation data = put
		.put(Fields.allowedUser, allowedUser);
		
		
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,data
				);
		return createBulkItems;
	}
}
