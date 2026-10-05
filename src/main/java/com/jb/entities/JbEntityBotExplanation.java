package com.jb.entities;

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
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jb.business.bots.engine.JbBotType;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

import com.jn.utils.JnLanguage;

/**
 * The explanation of a bot in a language, sent by {@code explainThisBot}.
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code jb_bot_explanation}</li>
 * <li>records cached for 3600 seconds</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotExplanation.Fields.class)
public class JbEntityBotExplanation implements CcpEntityConfigurator {

	/** The entity {@code jb_bot_explanation}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotExplanation.class).entityInstance;
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code botName} field: part of the primary key, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		botName, 
		/** The {@code language} field: part of the primary key, validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		language,
		/** The {@code message} field: required, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		message, 
		;
		
	}
	
	/**
	 * Seeds the Portuguese explanations of the support and user bots.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
		.put(JnJsonInstantMessengerFields.message, "Bot de rotinas administrativas que só podem ser acessadas por usuários cadastrados");
		String portugueseName = JnLanguage.portuguese.name();
		CcpJsonRepresentation put2 = put
		.put(JnJsonCommonsFields.language, portugueseName);
		String supportName = JbBotType.support.name();

		CcpJsonRepresentation support = put2
		.put(JnJsonInstantMessengerFields.botName, supportName)
		;
		CcpJsonRepresentation put3 = CcpOtherConstants.EMPTY_JSON
		.put(JnJsonInstantMessengerFields.message, "Bot de rotinas públicas que podem ser acessadas por todos os usuários");
		String portugueseName2 = JnLanguage.portuguese.name();
		CcpJsonRepresentation put4 = put3
		.put(JnJsonCommonsFields.language, portugueseName2);
		String userName = JbBotType.user.name();
		CcpJsonRepresentation user = put4
		.put(JnJsonInstantMessengerFields.botName, userName)
		;
		
		
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,support
				,user
				);
		return createBulkItems;
	}

}
