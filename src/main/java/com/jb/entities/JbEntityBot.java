package com.jb.entities;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.jb.business.bots.engine.JbBotType;
import com.jb.business.bots.engine.JbBusinessReceiveBotUpdate;
import com.jb.business.bots.engine.JbCommandNamesInPortuguese;
import com.jb.business.bots.engine.JbDefaultBotCommandMessages;
import com.jb.business.bots.engine.JbLanguageNames;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsChoose;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsMessages;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsShow;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsStatus;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsSteps;
import com.jn.business.bots.JnBotConfigurationSeeds;
import com.jn.entities.JnEntityBotCommandStep;
import com.jn.entities.JnEntityBotCommandStepEndMessage;
import com.jn.entities.JnEntityBotUpdateHandler;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * A bot of the platform. Seeds the support bot and the part of its configuration that belongs to jb: the command
 * {@code pendingTickets}, the Portuguese names of the default commands and the texts of the engine. The commands themselves are records of {@code JnEntityBotCommand}, found by {@code botName} when the bot
 * starts; the other cost centers seed theirs (vis seeds the skill and ignored users commands, js the login token tickets), and jb does not know them.
 * Until 2026-10-15 this entity kept the list of the commands of the bot, seeded only by jb, which made jb import vis.
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code jb_bot}</li>
 * <li>records cached for 3600 seconds</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(JbEntityBot.Fields.class)
public class JbEntityBot implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static JbEntityBot INSTANCE = new JbEntityBot();

	private JbEntityBot() {}

	/** The entity {@code jb_bot}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(JbEntityBot.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code botName} field: part of the primary key, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		botName,
		;
	}


	/**
	 * Seeds the support bot, the configuration of the commands of jb, the texts of the support bot that belong to jb
	 * (Portuguese and English) and the business that receives the updates of the webhook.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		CcpJsonRepresentation supportBot = CcpOtherConstants.EMPTY_JSON.put(JnJsonInstantMessengerFields.botName, JbBotType.support);
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(ENTITY, supportBot);
		List<CcpBulkItem> firstRecords = new ArrayList<>(createBulkItems);

		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JbBotType.support);
		JnBotConfigurationSeeds withPendingTickets = this.addPendingTickets(seeds);
		JnBotConfigurationSeeds withEngineTexts = this.addEngineTexts(withPendingTickets);
		JbCommandNamesInPortuguese[] commandNamesInPortuguese = JbCommandNamesInPortuguese.values();
		JnBotConfigurationSeeds withNames = withEngineTexts.portugueseNames(commandNamesInPortuguese);
		List<CcpBulkItem> botItems = withNames.getBulkItems();
		firstRecords.addAll(botItems);

		String supportName = JbBotType.support.name();
		List<CcpBulkItem> updateHandlerSeeds = JnEntityBotUpdateHandler.getSeeds(supportName, JbBusinessReceiveBotUpdate.class);
		firstRecords.addAll(updateHandlerSeeds);

		return firstRecords;
	}

	/**
	 * {@code /pendingTickets}: shows how many tickets the operator has and the oldest one, and goes on to the choice
	 * between solving it (ending the session, after which the bot starts the command of the ticket) and going to the
	 * next one. Without any ticket the session ends with that notice; an answer not understood repeats the choice.
	 * @param seeds the seeds so far
	 * @return the seeds with the command
	 */
	private JnBotConfigurationSeeds addPendingTickets(JnBotConfigurationSeeds seeds) {
		String commandName = JbSupportBotCommands.pendingTickets.name();
		String choose = JbSupportPendingTicketsSteps.pendingTicketsChoose.name();
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		int noPendingTicket = JbSupportPendingTicketsStatus.noPendingTicket.asNumber();
		CcpJsonRepresentation noPendingTicketFlowOfTheList = JnEntityBotCommandStep.getStepFlow(noPendingTicket, "", botReply);
		CcpJsonRepresentation showStep = JnEntityBotCommandStep.getBotCommandStep(commandName, JbSupportPendingTicketsShow.class, choose, noPendingTicketFlowOfTheList);

		int invalidAnswer = JbSupportPendingTicketsStatus.invalidAnswer.asNumber();
		int ticketChosen = JbSupportPendingTicketsStatus.ticketChosen.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, choose, botReply);
		CcpJsonRepresentation noPendingTicketFlow = JnEntityBotCommandStep.getStepFlow(noPendingTicket, "", botReply);
		CcpJsonRepresentation ticketChosenFlow = JnEntityBotCommandStep.getStepFlow(ticketChosen, "", botReply);
		CcpJsonRepresentation chooseStep = JnEntityBotCommandStep.getBotCommandStep(choose, JbSupportPendingTicketsChoose.class, choose, invalidAnswerFlow, noPendingTicketFlow, ticketChosenFlow);

		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName, choose);

		JnBotConfigurationSeeds withCommand = seeds
				.command(commandName)
				.steps(showStep, chooseStep)
				.endMessages(endMessages)
				.systemMessage(JbSupportPendingTicketsMessages.oneTicket
						, "Você tem 1 ticket para resolver"
						, "You have 1 ticket to solve")
				.systemMessage(JbSupportPendingTicketsMessages.manyTickets
						, "Você tem {ticketsCount} tickets para resolver"
						, "You have {ticketsCount} tickets to solve")
				.systemMessage(JbSupportPendingTicketsMessages.ticketPrompt
						, "Ticket {ticketNumber} de {ticketsCount} => {ticket}\n"
						+ "Digite \"1\" para resolver ou \"2\" para ir ao próximo"
						, "Ticket {ticketNumber} of {ticketsCount} => {ticket}\n"
						+ "Type \"1\" to solve it or \"2\" to go to the next one")
				.systemMessage(JbSupportPendingTicketsMessages.noPendingTicket
						, "Você não tem tickets para resolver."
						, "You have no tickets to solve.")
				.systemMessage(JbSupportPendingTicketsMessages.notUnderstood
						, "Não entendi a resposta.\n\n"
						, "I did not understand the answer.\n\n")
				.systemMessage(JbSupportPendingTicketsMessages.ticketAlreadySolved
						, "O ticket {ticket} já foi resolvido.\n\n"
						, "The ticket {ticket} was already solved.\n\n")
				.systemMessage(JbSupportPendingTicketsMessages.ticketChosen
						, "Resolvendo o ticket {ticket}"
						, "Solving the ticket {ticket}");
		return withCommand;
	}

	/**
	 * The texts of the default commands of every bot and the names of the languages.
	 * @param seeds the seeds so far
	 * @return the seeds with the texts
	 */
	private JnBotConfigurationSeeds addEngineTexts(JnBotConfigurationSeeds seeds) {
		JnBotConfigurationSeeds withTexts = seeds
				.systemMessage(JbDefaultBotCommandMessages.commandExited
						, "Você saiu do comando {commandName}. Agora você pode executar outro comando."
						, "You left the command {commandName}. Now you can run another command.")
				.systemMessage(JbDefaultBotCommandMessages.languageSet
						, "Idioma definido: {languageName}."
						, "Language set: {languageName}.")
				.systemMessage(JbDefaultBotCommandMessages.chooseLanguage
						, "Seu idioma atual é {languageName}. Para trocar, digite {commandName} seguido de um destes idiomas: {languages}"
						, "Your current language is {languageName}. To change it, type {commandName} followed by one of these languages: {languages}")
				.systemMessage(JbDefaultBotCommandMessages.unknownLanguage
						, "Idioma não reconhecido: {typedLanguage}. Digite {commandName} seguido de um destes idiomas: {languages}"
						, "Unknown language: {typedLanguage}. Type {commandName} followed by one of these languages: {languages}")
				.systemMessage(JbLanguageNames.portuguese, "português", "Portuguese")
				.systemMessage(JbLanguageNames.english, "inglês", "English")
				.systemMessage(JbLanguageNames.spanish, "espanhol", "Spanish");
		return withTexts;
	}

}
