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
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsChoose;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsShow;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsStatus;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsSteps;
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

/**
 * A step of a bot command: the business it runs ({@code engine}), the step that follows ({@code nextStep}) and, by status of a {@code CcpErrorFlowDisturb} thrown by the engine, the messages to send and the step to go to ({@code stepFlow}).
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code jb_bot_command_step}</li>
 * <li>records cached for 3600 seconds</li>
 * <li>versionable: every write keeps the previous state in {@code jn_versionable}</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),})
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBotCommandStep.Fields.class)
public class JbEntityBotCommandStep implements CcpEntityConfigurator {

	/** The entity {@code jb_bot_command_step}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBotCommandStep.class).entityInstance;
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code stepName} field: part of the primary key, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		stepName, 
		/** The {@code stepFlow} field: required, list, nested JSON. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeNestedJson(jsonValidation = JbNextStepFields.class)
		stepFlow,
		/** The {@code engine} field: required, text. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		engine,
		/** The {@code nextStep} field: text. */
		@CcpJsonFieldTypeString
		nextStep,
		;
	}
	
	
	/**
	 * Builds a complete record of this entity, ready for {@code ENTITY.save(...)}. The {@code stepFlow} items come from
	 * {@link #getStepFlow(Integer, String, CcpJsonRepresentation...)} and there must be at least one, as
	 * {@code @CcpJsonFieldValidatorArray(minSize = 1)} requires. {@code nextStep} is left out of the JSON when blank, by the
	 * same criterion as {@link #getStepFlow(Integer, String, CcpJsonRepresentation...)}.
	 * @param stepName the name of the step
	 * @param engine the business run by the step
	 * @param nextStep the step that follows, blank for none
	 * @param stepFlow the flow items
	 * @return the record
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
	 * Builds a {@code stepFlow} item, in the format of {@code JbNextStepFields}: the status that triggers the diversion, the
	 * alternative step and the messages sent to the user. The messages come from
	 * {@link #getStepFlowMessage(JnLanguage, String)}. Both {@code message} (empty varargs) and {@code nextStep} (blank) are
	 * left out of the JSON: writing them empty would break the {@code @CcpJsonFieldValidatorArray(minSize = 1)} of the first
	 * and wrongly satisfy the {@code requiresAtLeastOne} between the two.
	 * @param status the status
	 * @param nextStep the alternative step, blank for none
	 * @param message the messages
	 * @return the flow item
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
	 * Builds a message of a {@code stepFlow} item, in the format of {@code JbNextStepMessageFields}: the language and the
	 * text sent to the user.
	 * @param language the language
	 * @param message the text
	 * @return the message
	 */
	public static CcpJsonRepresentation getStepFlowMessage(JnLanguage language, String message) {

		String languageName = language.name();

		CcpJsonRepresentation putLanguage = CcpOtherConstants.EMPTY_JSON.put(JbNextStepMessageFields.language, languageName);
		CcpJsonRepresentation stepFlowMessage = putLanguage.put(JbNextStepMessageFields.message, message);

		return stepFlowMessage;
	}

	/**
	 * Seeds the steps of the support commands, with their engines, next steps and flow items.
	 * @return the seed records
	 */
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
				, this.getAllowCommandToUserStep()
				, this.getPendingTicketsStep()
				, this.getChoosePendingTicketStep());

		return createBulkItems;
	}

	/**
	 * The flow messages of the {@code fixSkillHierarchy} and {@code pendingTickets} steps are the text the engine of the step left in
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
	 * {@code /fixSkillHierarchy <parent> <type> <email>}: shows the request and goes on to the choice of how to decide
	 * it. Without any item the session ends with a notice to the operator; when every item was decided in
	 * earlier reviews, the review finishes right away with its summary; a user ignored for the command is not
	 * reviewed, and the operator is told how to stop ignoring them.
	 */
	private CcpJsonRepresentation getShowSkillFixHierarchyRequestStep() {
		String commandName = JbSupportBotCommands.fixSkillHierarchy.name();
		String chooseMode = JbSupportSkillFixHierarchySteps.fixSkillHierarchyChooseMode.name();
		String email = "{" + VisEntitySkillFixHierarchyPending.Fields.email + "}";
		String parent = "{" + VisEntitySkillFixHierarchyPending.Fields.parent + "}";
		String type = "{" + VisEntitySkillFixHierarchyPending.Fields.type + "}";

		CcpJsonRepresentation portuguese = getStepFlowMessage(JnLanguage.portuguese, "Não há itens pendentes de ajuste (" + type + ") na hierarquia de conhecimentos para o e-mail '" + email + "' e o termo '" + parent + "'");
		CcpJsonRepresentation english = getStepFlowMessage(JnLanguage.english, "There are no pending skill hierarchy fix items (" + type + ") for the e-mail '" + email + "' and the term '" + parent + "'");
		CcpJsonRepresentation spanish = getStepFlowMessage(JnLanguage.spanish, "No hay ítems pendientes de ajuste (" + type + ") en la jerarquía de conocimientos para el correo '" + email + "' y el término '" + parent + "'");
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
	 * {@code /pendingTickets}: shows how many tickets the operator has and the oldest one, and goes on to the
	 * choice between solving it and going to the next one. Without any ticket the session ends with that notice.
	 */
	private CcpJsonRepresentation getPendingTicketsStep() {
		String commandName = JbSupportBotCommands.pendingTickets.name();
		String choose = JbSupportPendingTicketsSteps.pendingTicketsChoose.name();
		CcpJsonRepresentation[] botReply = this.getBotReplyInEveryLanguage();

		int noPendingTicket = JbSupportPendingTicketsStatus.noPendingTicket.asNumber();
		CcpJsonRepresentation noPendingTicketFlow = getStepFlow(noPendingTicket, "", botReply);

		CcpJsonRepresentation step = getBotCommandStep(commandName, JbSupportPendingTicketsShow.class, choose, noPendingTicketFlow);
		return step;
	}

	/**
	 * Solve the ticket shown (ending the session, after which the bot starts the command of the ticket) or go to
	 * the next one (repeating this step). An answer not understood repeats this step, and a list that became
	 * empty in the meantime ends the session.
	 */
	private CcpJsonRepresentation getChoosePendingTicketStep() {
		String choose = JbSupportPendingTicketsSteps.pendingTicketsChoose.name();
		CcpJsonRepresentation[] botReply = this.getBotReplyInEveryLanguage();

		int invalidAnswer = JbSupportPendingTicketsStatus.invalidAnswer.asNumber();
		int noPendingTicket = JbSupportPendingTicketsStatus.noPendingTicket.asNumber();
		int ticketChosen = JbSupportPendingTicketsStatus.ticketChosen.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = getStepFlow(invalidAnswer, choose, botReply);
		CcpJsonRepresentation noPendingTicketFlow = getStepFlow(noPendingTicket, "", botReply);
		CcpJsonRepresentation ticketChosenFlow = getStepFlow(ticketChosen, "", botReply);

		CcpJsonRepresentation step = getBotCommandStep(choose, JbSupportPendingTicketsChoose.class, choose, invalidAnswerFlow, noPendingTicketFlow, ticketChosenFlow);
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


