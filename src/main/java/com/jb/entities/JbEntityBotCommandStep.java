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
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.process.CcpProcessStatusDefault;
import com.jb.business.bots.command.allowed.JbSupportAllowCommandToUser;
import com.jb.business.bots.command.allowed.JbSupportAllowCommandToUserStatus;
import com.jb.business.bots.command.allowed.JbSupportAllowCommandToUserFields;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.login.token.JbSupportLoginToken;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyChooseMode;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyConfirmIgnore;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyDecideItem;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyFields;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyShowRequest;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyStatus;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchySteps;
import com.jb.entities.subfields.JbNextStepFields;
import com.jb.entities.subfields.JbNextStepMessageFields;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionablePurgeBuilder;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),})
@JnEntityVersionable(JnVersionableEntity.class)
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


		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(ENTITY, solveLoginTokenTicketCommand
				, this.getShowSkillFixHierarchyRequestStep()
				, this.getChooseSkillFixHierarchyReviewModeStep()
				, this.getDecideSkillFixHierarchyItemStep()
				, this.getConfirmSkillFixHierarchyIgnoreStep()
				, this.getAllowCommandToUserStep());

		return createBulkItems;
	}

	/**
	 * The flow messages of the {@code fixSkillHierarchy} steps are the text the engine of the step left in
	 * {@code botReply}, whatever the language: the engine already writes it in the language of the session.
	 */
	private CcpJsonRepresentation[] getBotReplyInEveryLanguage() {
		String botReply = "{" + JbSupportSkillFixHierarchyFields.botReply + "}";
		CcpJsonRepresentation portuguese = getStepFlowMessage(JnLanguage.portuguese, botReply);
		CcpJsonRepresentation english = getStepFlowMessage(JnLanguage.english, botReply);
		CcpJsonRepresentation spanish = getStepFlowMessage(JnLanguage.spanish, botReply);
		CcpJsonRepresentation[] botReplyInEveryLanguage = {portuguese, english, spanish};
		return botReplyInEveryLanguage;
	}

	/**
	 * {@code /fixSkillHierarchy <parent> <email>}: shows the request and goes on to the choice of how to decide
	 * it. Without any item the session ends with a notice to the operator; when every item was decided in
	 * earlier reviews, the review finishes right away with its summary; a user ignored for the command is not
	 * reviewed, and the operator is told how to stop ignoring them.
	 */
	private CcpJsonRepresentation getShowSkillFixHierarchyRequestStep() {
		String commandName = JbSupportBotCommands.fixSkillHierarchy.name();
		String chooseMode = JbSupportSkillFixHierarchySteps.fixSkillHierarchyChooseMode.name();
		String email = "{" + VisEntitySkillFixHierarchyPending.Fields.email + "}";
		String parent = "{" + VisEntitySkillFixHierarchyPending.Fields.parent + "}";

		CcpJsonRepresentation portuguese = getStepFlowMessage(JnLanguage.portuguese, "Não há itens pendentes de ajuste na hierarquia de conhecimentos para o e-mail '" + email + "' e o termo '" + parent + "'");
		CcpJsonRepresentation english = getStepFlowMessage(JnLanguage.english, "There are no pending skill hierarchy fix items for the e-mail '" + email + "' and the term '" + parent + "'");
		CcpJsonRepresentation spanish = getStepFlowMessage(JnLanguage.spanish, "No hay ítems pendientes de ajuste en la jerarquía de conocimientos para el correo '" + email + "' y el término '" + parent + "'");
		int requestNotFound = JbSupportSkillFixHierarchyStatus.requestNotFound.asNumber();
		CcpJsonRepresentation requestNotFoundFlow = getStepFlow(requestNotFound, "", portuguese, english, spanish);

		CcpJsonRepresentation[] botReply = this.getBotReplyInEveryLanguage();
		int reviewFinished = JbSupportSkillFixHierarchyStatus.reviewFinished.asNumber();
		CcpJsonRepresentation reviewFinishedFlow = getStepFlow(reviewFinished, "", botReply);

		String allowCommandToUser = "/" + JbSupportBotCommands.allowCommandToUser + " " + commandName + " " + email;
		CcpJsonRepresentation portugueseNotAllowed = getStepFlowMessage(JnLanguage.portuguese, "O usuário " + email + " está sendo ignorado no comando " + commandName + " e as solicitações dele não são atendidas. Para voltar a atendê-lo, use " + allowCommandToUser);
		CcpJsonRepresentation englishNotAllowed = getStepFlowMessage(JnLanguage.english, "The user " + email + " is being ignored in the command " + commandName + " and their requests are not reviewed. To review them again, use " + allowCommandToUser);
		CcpJsonRepresentation spanishNotAllowed = getStepFlowMessage(JnLanguage.spanish, "El usuario " + email + " está siendo ignorado en el comando " + commandName + " y sus solicitudes no se atienden. Para volver a atenderlo, use " + allowCommandToUser);
		int userNotAllowed = JbSupportSkillFixHierarchyStatus.userNotAllowed.asNumber();
		CcpJsonRepresentation userNotAllowedFlow = getStepFlow(userNotAllowed, "", portugueseNotAllowed, englishNotAllowed, spanishNotAllowed);

		CcpJsonRepresentation step = getBotCommandStep(commandName, JbSupportSkillFixHierarchyShowRequest.class, chooseMode, requestNotFoundFlow, reviewFinishedFlow, userNotAllowedFlow);
		return step;
	}

	/**
	 * Approve all, reject all (finishing the review), one by one (going on to the item decision) or ignore the
	 * user (going on to its confirmation). An answer not understood repeats this step.
	 */
	private CcpJsonRepresentation getChooseSkillFixHierarchyReviewModeStep() {
		String chooseMode = JbSupportSkillFixHierarchySteps.fixSkillHierarchyChooseMode.name();
		String decideItem = JbSupportSkillFixHierarchySteps.fixSkillHierarchyDecideItem.name();
		String confirmIgnore = JbSupportSkillFixHierarchySteps.fixSkillHierarchyConfirmIgnore.name();
		CcpJsonRepresentation[] botReply = this.getBotReplyInEveryLanguage();

		int invalidAnswer = JbSupportSkillFixHierarchyStatus.invalidAnswer.asNumber();
		int reviewFinished = JbSupportSkillFixHierarchyStatus.reviewFinished.asNumber();
		int ignoreConfirmationAsked = JbSupportSkillFixHierarchyStatus.ignoreConfirmationAsked.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = getStepFlow(invalidAnswer, chooseMode, botReply);
		CcpJsonRepresentation reviewFinishedFlow = getStepFlow(reviewFinished, "", botReply);
		CcpJsonRepresentation ignoreConfirmationAskedFlow = getStepFlow(ignoreConfirmationAsked, confirmIgnore, botReply);

		CcpJsonRepresentation step = getBotCommandStep(chooseMode, JbSupportSkillFixHierarchyChooseMode.class, decideItem, invalidAnswerFlow, reviewFinishedFlow, ignoreConfirmationAskedFlow);
		return step;
	}

	/**
	 * {@code /allowCommandToUser <command> <email>}: the user stops being ignored for the command. When the user
	 * was not ignored for it, the session ends with a notice to the operator.
	 */
	private CcpJsonRepresentation getAllowCommandToUserStep() {
		String commandName = JbSupportBotCommands.allowCommandToUser.name();
		String email = "{" + JnJsonCommonsFields.email + "}";
		String command = "{" + JbSupportAllowCommandToUserFields.command + "}";

		CcpJsonRepresentation portuguese = getStepFlowMessage(JnLanguage.portuguese, "O usuário " + email + " não está sendo ignorado no comando " + command);
		CcpJsonRepresentation english = getStepFlowMessage(JnLanguage.english, "The user " + email + " is not being ignored in the command " + command);
		CcpJsonRepresentation spanish = getStepFlowMessage(JnLanguage.spanish, "El usuario " + email + " no está siendo ignorado en el comando " + command);
		int userNotIgnored = JbSupportAllowCommandToUserStatus.userNotIgnored.asNumber();
		CcpJsonRepresentation userNotIgnoredFlow = getStepFlow(userNotIgnored, "", portuguese, english, spanish);

		CcpJsonRepresentation step = getBotCommandStep(commandName, JbSupportAllowCommandToUser.class, "", userNotIgnoredFlow);
		return step;
	}

	/**
	 * Confirmation of the intention to ignore the user: yes ends the session with the user ignored, no goes back
	 * to the choice of how to decide the items, and an answer not understood repeats this step.
	 */
	private CcpJsonRepresentation getConfirmSkillFixHierarchyIgnoreStep() {
		String chooseMode = JbSupportSkillFixHierarchySteps.fixSkillHierarchyChooseMode.name();
		String confirmIgnore = JbSupportSkillFixHierarchySteps.fixSkillHierarchyConfirmIgnore.name();
		CcpJsonRepresentation[] botReply = this.getBotReplyInEveryLanguage();

		int invalidAnswer = JbSupportSkillFixHierarchyStatus.invalidAnswer.asNumber();
		int userIgnored = JbSupportSkillFixHierarchyStatus.userIgnored.asNumber();
		int ignoreCanceled = JbSupportSkillFixHierarchyStatus.ignoreCanceled.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = getStepFlow(invalidAnswer, confirmIgnore, botReply);
		CcpJsonRepresentation userIgnoredFlow = getStepFlow(userIgnored, "", botReply);
		CcpJsonRepresentation ignoreCanceledFlow = getStepFlow(ignoreCanceled, chooseMode, botReply);

		CcpJsonRepresentation step = getBotCommandStep(confirmIgnore, JbSupportSkillFixHierarchyConfirmIgnore.class, "", invalidAnswerFlow, userIgnoredFlow, ignoreCanceledFlow);
		return step;
	}

	/**
	 * Decision on one item; the step repeats itself until the last item, which finishes the review.
	 */
	private CcpJsonRepresentation getDecideSkillFixHierarchyItemStep() {
		String decideItem = JbSupportSkillFixHierarchySteps.fixSkillHierarchyDecideItem.name();
		CcpJsonRepresentation[] botReply = this.getBotReplyInEveryLanguage();

		int invalidAnswer = JbSupportSkillFixHierarchyStatus.invalidAnswer.asNumber();
		int reviewFinished = JbSupportSkillFixHierarchyStatus.reviewFinished.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = getStepFlow(invalidAnswer, decideItem, botReply);
		CcpJsonRepresentation reviewFinishedFlow = getStepFlow(reviewFinished, "", botReply);

		CcpJsonRepresentation step = getBotCommandStep(decideItem, JbSupportSkillFixHierarchyDecideItem.class, decideItem, invalidAnswerFlow, reviewFinishedFlow);
		return step;
	}
}


