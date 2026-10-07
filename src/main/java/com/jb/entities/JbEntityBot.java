package com.jb.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import com.jb.business.bots.engine.JbBotType;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jb.business.bots.login.token.JbSupportLoginTokenTypes;
import com.jb.business.bots.pending.tickets.JbSupportPendingTicketsMessages;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyMessages;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.messages.JnSystemMessage;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisSkillFixHierarchyDecisions;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

import java.util.stream.Stream;

/**
 * A bot of the platform and the names of its  commands, by language. Read once by {@code JbBotEngine} when it starts.
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code jb_bot}</li>
 * <li>records cached for 3600 seconds</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = JbEntityBot.Fields.class)
public class JbEntityBot implements CcpEntityConfigurator {

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

		/** The {@code commandName} field: required, list, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		commandName
		;
	}
	
	
	/**
	 * Seeds the support bot with every command of {@code JbSupportBotCommands}, the templates of the login token ticket
	 * notices and the system messages of the support bot (Portuguese and English).
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		JbSupportBotCommands[] jbSupportBotCommandsValues = JbSupportBotCommands.values();
		Stream<JbSupportBotCommands> stream = Arrays.asList(jbSupportBotCommandsValues).stream();
		var streamMap = stream.map(x -> x.name());

		List<String> commandName = streamMap.collect(Collectors.toList());
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
		.put(JnJsonInstantMessengerFields.botName, JbBotType.support);

		CcpJsonRepresentation supportBot = put
		.put(JnJsonInstantMessengerFields.commandName, commandName)
		;
		
		List<CcpBulkItem> createBulkItems = CcpEntityConfigurator.super.toCreateBulkItems(
				ENTITY
				,supportBot
				);
		ArrayList<CcpBulkItem> arrayList = new ArrayList<>(createBulkItems);
		JbSupportLoginTokenTypes[] values = JbSupportLoginTokenTypes.values();
		
		for (JbSupportLoginTokenTypes value : values) {
			List<CcpBulkItem> instantMessageTemplate = value.getInstantMessageTemplate();
			arrayList.addAll(instantMessageTemplate);
		}

		List<CcpBulkItem> systemMessages = this.getSystemMessages();
		arrayList.addAll(systemMessages);

		return arrayList;
	}

	/**
	 * Texts of the support bot conversations, one {@link JnEntitySystemMessage} record per item and language: the
	 * descriptions of {@link VisSkillFixHierarchyTypes}, the group titles of {@link VisSkillFixHierarchyDecisions}
	 * and the texts of {@link JbSupportSkillFixHierarchyMessages} (each {@code {field}} of a template is filled when
	 * the text is read).
	 */
	private List<CcpBulkItem> getSystemMessages() {
		List<CcpBulkItem> systemMessages = new ArrayList<>();

		this.addSystemMessage(systemMessages, VisSkillFixHierarchyTypes.add, "associação", "association");
		this.addSystemMessage(systemMessages, VisSkillFixHierarchyTypes.remove, "desassociação", "dissociation");

		this.addSystemMessage(systemMessages, VisSkillFixHierarchyDecisions.approved, "Itens aprovados", "Approved items");
		this.addSystemMessage(systemMessages, VisSkillFixHierarchyDecisions.rejected, "Itens reprovados", "Rejected items");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.options
				, "Responda com uma das opções:\n"
				+ "• aprovar <justificativa> — aprova todos os itens pendentes\n"
				+ "• rejeitar <justificativa> — rejeita todos os itens pendentes\n"
				+ "• um a um — decide item por item\n"
				+ "• ignorar — descarta a solicitação e ignora as próximas deste usuário neste comando"
				, "Answer with one of the options:\n"
				+ "• approve <justification> — approves all the pending items\n"
				+ "• reject <justification> — rejects all the pending items\n"
				+ "• one by one — decides item by item\n"
				+ "• ignore — discards the request and ignores the next ones of this user in this command");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.ignoreConfirmation
				, "Confirma que o usuário {email} será ignorado no comando fixSkillHierarchy? "
				+ "Esta solicitação será descartada sem aviso ao usuário e as próximas não chegarão mais a você.\n"
				+ "Responda: sim ou não"
				, "Do you confirm that the user {email} will be ignored in the fixSkillHierarchy command? "
				+ "This request will be discarded without notifying the user and the next ones will no longer reach you.\n"
				+ "Answer: yes or no");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.previousDecisionApproved
				, "item já aprovado em revisão anterior"
				, "item already approved in an earlier review");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.previousDecisionRejected
				, "item já reprovado em revisão anterior"
				, "item already rejected in an earlier review");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.notUnderstood
				, "Não entendi a resposta. Toda decisão precisa vir acompanhada de uma justificativa.\n\n"
				, "I did not understand the answer. Every decision needs to come with a justification.\n\n");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.itemPrompt
				, "Item {itemNumber} de {itemsCount}: {skill} ({typeDescription} com o termo {parent})\n"
				+ "Responda: aprovar <justificativa> ou rejeitar <justificativa>"
				, "Item {itemNumber} of {itemsCount}: {skill} ({typeDescription} with the term {parent})\n"
				+ "Answer: approve <justification> or reject <justification>");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.reviewFinished
				, "Revisão concluída para {email} / {parent}.\n"
				+ "Aprovados: {approvedSkills}\n"
				+ "Reprovados: {rejectedSkills}\n"
				+ "O usuário será avisado por e-mail."
				, "Review finished for {email} / {parent}.\n"
				+ "Approved: {approvedSkills}\n"
				+ "Rejected: {rejectedSkills}\n"
				+ "The user will be notified by email.");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.noSkill, "-", "-");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.userIgnored
				, "O usuário {email} foi ignorado no comando fixSkillHierarchy. A solicitação para o termo {parent} foi descartada."
				, "The user {email} was ignored in the fixSkillHierarchy command. The request for the term {parent} was discarded.");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.ignoreCanceled
				, "O usuário não será ignorado.\n\n{options}"
				, "The user will not be ignored.\n\n{options}");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.ignoreConfirmationNotUnderstood
				, "Não entendi a resposta.\n\n{ignoreConfirmation}"
				, "I did not understand the answer.\n\n{ignoreConfirmation}");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.requestHeader
				, "Solicitação de {typeDescription} de {email} para o termo {parent}\n\n"
				, "Request of {typeDescription} from {email} for the term {parent}\n\n");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.requestTypeJustification
				, "[{typeDescription}]\nJustificativa do usuário: {description}\n"
				, "[{typeDescription}]\nUser's justification: {description}\n");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.pendingItems
				, "Itens pendentes: {skills}\n"
				, "Pending items: {skills}\n");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.approvedBefore
				, "Já aprovados anteriormente (não serão perguntados): {skills}\n"
				, "Already approved before (will not be asked): {skills}\n");

		this.addSystemMessage(systemMessages, JbSupportSkillFixHierarchyMessages.rejectedBefore
				, "Já reprovados anteriormente (não serão perguntados): {skills}\n"
				, "Already rejected before (will not be asked): {skills}\n");

		List<CcpBulkItem> pendingTicketsMessages = this.getPendingTicketsMessages();
		systemMessages.addAll(pendingTicketsMessages);

		return systemMessages;
	}

	/**
	 * Texts of the {@code pendingTickets} command ({@link JbSupportPendingTicketsMessages}).
	 */
	private List<CcpBulkItem> getPendingTicketsMessages() {
		List<CcpBulkItem> systemMessages = new ArrayList<>();

		this.addSystemMessage(systemMessages, JbSupportPendingTicketsMessages.oneTicket
				, "Você tem 1 ticket para resolver"
				, "You have 1 ticket to solve");

		this.addSystemMessage(systemMessages, JbSupportPendingTicketsMessages.manyTickets
				, "Você tem {ticketsCount} tickets para resolver"
				, "You have {ticketsCount} tickets to solve");

		this.addSystemMessage(systemMessages, JbSupportPendingTicketsMessages.ticketPrompt
				, "Ticket {ticketNumber} de {ticketsCount} => {ticket}\n"
				+ "Digite \"1\" para resolver ou \"2\" para ir ao próximo"
				, "Ticket {ticketNumber} of {ticketsCount} => {ticket}\n"
				+ "Type \"1\" to solve it or \"2\" to go to the next one");

		this.addSystemMessage(systemMessages, JbSupportPendingTicketsMessages.noPendingTicket
				, "Você não tem tickets para resolver."
				, "You have no tickets to solve.");

		this.addSystemMessage(systemMessages, JbSupportPendingTicketsMessages.notUnderstood
				, "Não entendi a resposta.\n\n"
				, "I did not understand the answer.\n\n");

		this.addSystemMessage(systemMessages, JbSupportPendingTicketsMessages.ticketAlreadySolved
				, "O ticket {ticket} já foi resolvido.\n\n"
				, "The ticket {ticket} was already solved.\n\n");

		this.addSystemMessage(systemMessages, JbSupportPendingTicketsMessages.ticketChosen
				, "Resolvendo o ticket {ticket}"
				, "Solving the ticket {ticket}");

		return systemMessages;
	}

	/**
	 * Adds the Portuguese and English records of a system message.
	 * @param systemMessages the records so far
	 * @param systemMessage the system message
	 * @param portugueseText the Portuguese text
	 * @param englishText the English text
	 */
	private void addSystemMessage(List<CcpBulkItem> systemMessages, JnSystemMessage systemMessage, String portugueseText, String englishText) {
		List<CcpBulkItem> inPortuguese = systemMessage.toBulkItems(JnLanguage.portuguese, portugueseText);
		List<CcpBulkItem> inEnglish = systemMessage.toBulkItems(JnLanguage.english, englishText);
		systemMessages.addAll(inPortuguese);
		systemMessages.addAll(inEnglish);
	}

}
