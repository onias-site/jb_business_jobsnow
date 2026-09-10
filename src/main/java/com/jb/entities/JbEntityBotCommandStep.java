package com.jb.entities;

import java.util.Arrays;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.process.CcpProcessStatusDefault;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.login.token.JbSupportLoginToken;
import com.jb.entities.subfields.JbNextStepFields;
import com.jb.entities.subfields.JbNextStepMessageFields;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;

@CcpEntityCache(3600)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandStep.Fields.class)
/**
 * Entidade que define um passo de um comando de bot: o motor de negócio a instanciar via
 * reflexão ({@code engine}), o próximo passo ({@code nextStep}) e o mapeamento de status de
 * erro para passos alternativos ({@code stepFlow}). Versionável, cache de 1 hora.
 */
public class JbEntityBotCommandStep implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandStep.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		stepName, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeNestedJson(jsonValidation = JbNextStepFields.class)
		stepFlow,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		engine,
		@CcpJsonFieldTypeString
		nextStep,
		;
	}
	
	
	/**
	 * Monta um registro completo desta entidade, pronto para {@code ENTITY.save(...)}. O parâmetro
	 * {@code stepFlow} é alimentado por {@link #getStepFlow(Integer, String, CcpJsonRepresentation...)}
	 * e precisa ter ao menos um item, conforme {@code @CcpJsonFieldValidatorArray(minSize = 1)}.
	 * {@code nextStep} é omitido do JSON quando vem em branco, pelo mesmo critério de
	 * {@link #getStepFlow(Integer, String, CcpJsonRepresentation...)}.
	 */
	public static CcpJsonRepresentation getBotCommandStep(String stepName, Class<?> engine, String nextStep, CcpJsonRepresentation... stepFlow) {

		String engineName = engine.getName();
		List<CcpJsonRepresentation> stepFlowList = Arrays.asList(stepFlow);

		CcpJsonRepresentation putStepName = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.stepName, stepName);
		CcpJsonRepresentation putEngine = putStepName.put(Fields.engine, engineName);
		CcpJsonRepresentation botCommandStep = putEngine.put(Fields.stepFlow, stepFlowList);

		String nextStepTrim = nextStep.trim();
		boolean hasNextStep = false == nextStepTrim.isEmpty();

		if(hasNextStep) {
			botCommandStep = botCommandStep.put(Fields.nextStep, nextStep);
		}

		return botCommandStep;
	}

	/**
	 * Monta um item do {@code stepFlow}, no formato definido por {@code JbNextStepFields}: o status de
	 * erro que dispara o desvio, o passo alternativo e as mensagens enviadas ao usuário. O parâmetro
	 * {@code message} é alimentado por {@link #getStepFlowMessage(JnLanguage, String)}. Tanto
	 * {@code message} (varargs vazio) quanto {@code nextStep} (string em branco) são omitidos do JSON:
	 * gravá-los vazios violaria o {@code @CcpJsonFieldValidatorArray(minSize = 1)} do primeiro e
	 * satisfaria indevidamente o {@code requiresAtLeastOne} entre os dois.
	 */
	public static CcpJsonRepresentation getStepFlow(Integer status, String nextStep, CcpJsonRepresentation... message) {

		List<CcpJsonRepresentation> messageList = Arrays.asList(message);

		CcpJsonRepresentation stepFlow = CcpOtherConstants.EMPTY_JSON .put(JbNextStepFields.status, status);

		String nextStepTrim = nextStep.trim();
		boolean hasNextStep = false == nextStepTrim.isEmpty();

		if(hasNextStep) {
			stepFlow = stepFlow.put(JbNextStepFields.nextStep, nextStep);
		}

		boolean hasMessage = false == messageList.isEmpty();

		if(hasMessage) {
			stepFlow = stepFlow.put(JbNextStepFields.message, messageList);
		}

		return stepFlow;
	}

	/**
	 * Monta uma mensagem de um item do {@code stepFlow}, no formato definido por
	 * {@code NextStepMessageFields}: o idioma e o texto enviado ao usuário.
	 */
	public static CcpJsonRepresentation getStepFlowMessage(JnLanguage language, String message) {

		String languageName = language.name();

		CcpJsonRepresentation putLanguage = CcpOtherConstants.EMPTY_JSON.put(JbNextStepMessageFields.language, languageName);
		CcpJsonRepresentation stepFlowMessage = putLanguage.put(JbNextStepMessageFields.message, message);

		return stepFlowMessage;
	}

	public List<CcpBulkItem> getFirstRecordsToInsert() {

		String solveLoginTokenTicket = JbSupportBotCommands.solveLoginTokenTicket.name();
		
		CcpJsonRepresentation portuguese = getStepFlowMessage(JnLanguage.portuguese, "O e-mail '{" 	+ JnJsonCommonsFields.email + "}' não possui aberto ticket do tipo '{" + JbSupportLoginToken.JsonFields.ticketType + "}'");
		CcpJsonRepresentation english = getStepFlowMessage(JnLanguage.english, "The e-mail '{" + JnJsonCommonsFields.email + "}' has no open ticket of the type '{" + JbSupportLoginToken.JsonFields.ticketType + "}'");
		CcpJsonRepresentation spanish = getStepFlowMessage(JnLanguage.spanish, "El correo '{" + JnJsonCommonsFields.email + "}' no tiene ticket abierto del tipo '{" + JbSupportLoginToken.JsonFields.ticketType + "}'");
		CcpJsonRepresentation stepFlow = getStepFlow(CcpProcessStatusDefault.NOT_FOUND.status, "", portuguese, english, spanish);

		CcpJsonRepresentation solveLoginTokenTicketCommand = getBotCommandStep(solveLoginTokenTicket, JbSupportLoginToken.class, "", stepFlow);


		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(ENTITY, solveLoginTokenTicketCommand);
		
		return createBulkItems;
	}
}


