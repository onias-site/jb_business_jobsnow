package com.jb.business.bots.skill.hierarchy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillFixHierarchyReview;
import com.vis.business.skill.VisSkillFixHierarchyDecisions;
import com.vis.business.skill.VisSkillFixHierarchyReviewFields;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Texts sent to the support bot operator along the {@code fixSkillHierarchy} command, in the language of the
 * session (Portuguese, or English for the other languages), and the end of the review, shared by the steps
 * that can finish it.
 */
final class JbSupportSkillFixHierarchyConversation {

	private JbSupportSkillFixHierarchyConversation() {}

	static JnLanguage getLanguage(CcpJsonRepresentation json) {
		// the language may be in the json as the enum itself or as its name
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		return language;
	}

	static boolean isPortuguese(JnLanguage language) {
		boolean portuguese = JnLanguage.portuguese == language;
		return portuguese;
	}

	static String getOptions(JnLanguage language) {

		boolean portuguese = isPortuguese(language);

		if(portuguese) {
			return "Responda com uma das opções:\n"
					+ "• aprovar <justificativa> — aprova todos os itens\n"
					+ "• rejeitar <justificativa> — rejeita todos os itens\n"
					+ "• um a um — decide item por item";
		}

		return "Answer with one of the options:\n"
				+ "• approve <justification> — approves all the items\n"
				+ "• reject <justification> — rejects all the items\n"
				+ "• one by one — decides item by item";
	}

	static String getNotUnderstood(JnLanguage language) {

		boolean portuguese = isPortuguese(language);

		if(portuguese) {
			return "Não entendi a resposta. Toda decisão precisa vir acompanhada de uma justificativa.\n\n";
		}

		return "I did not understand the answer. Every decision needs to come with a justification.\n\n";
	}

	/**
	 * Asks the operator to decide the item at the given position.
	 */
	static String getItemPrompt(CcpJsonRepresentation json, int itemIndex) {

		JnLanguage language = getLanguage(json);
		List<CcpJsonRepresentation> reviewItems = json.getAsJsonList(JbSupportSkillFixHierarchyFields.reviewItems);
		CcpJsonRepresentation item = reviewItems.get(itemIndex);
		String skill = item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
		VisSkillFixHierarchyTypes type = item.getAsEnum(VisEntitySkillFixHierarchyItemPending.Fields.type, VisSkillFixHierarchyTypes.class);
		String typeDescription = type.getDescription(language);
		String parent = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);
		int itemNumber = itemIndex + 1;
		int itemsCount = reviewItems.size();

		boolean portuguese = isPortuguese(language);

		if(portuguese) {
			return "Item " + itemNumber + " de " + itemsCount + ": " + skill + " (" + typeDescription + " com o termo " + parent + ")\n"
					+ "Responda: aprovar <justificativa> ou rejeitar <justificativa>";
		}

		return "Item " + itemNumber + " of " + itemsCount + ": " + skill + " (" + typeDescription + " with the term " + parent + ")\n"
				+ "Answer: approve <justification> or reject <justification>";
	}

	/**
	 * Applies the decisions (moves the items and the request, which emails the user) and ends the session with
	 * the summary of the review.
	 */
	static CcpJsonRepresentation finish(CcpJsonRepresentation json, List<CcpJsonRepresentation> decisions) {

		CcpJsonRepresentation requestKey = json.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent);
		CcpJsonRepresentation review = requestKey.put(VisSkillFixHierarchyReviewFields.reviewDecisions, decisions);
		VisBusinessSkillFixHierarchyReview.INSTANCE.execute(review);

		JnLanguage language = getLanguage(json);
		String approvedSkills = getSkillsWithTheDecision(decisions, VisSkillFixHierarchyDecisions.approved);
		String rejectedSkills = getSkillsWithTheDecision(decisions, VisSkillFixHierarchyDecisions.rejected);
		String email = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);
		String parent = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);

		boolean portuguese = isPortuguese(language);
		String summary = portuguese
				? "Revisão concluída para " + email + " / " + parent + ".\n"
						+ "Aprovados: " + approvedSkills + "\n"
						+ "Reprovados: " + rejectedSkills + "\n"
						+ "O usuário será avisado por e-mail."
				: "Review finished for " + email + " / " + parent + ".\n"
						+ "Approved: " + approvedSkills + "\n"
						+ "Rejected: " + rejectedSkills + "\n"
						+ "The user will be notified by email.";

		CcpJsonRepresentation jsonWithDecisions = json.put(VisSkillFixHierarchyReviewFields.reviewDecisions, decisions);
		CcpJsonRepresentation jsonWithSummary = jsonWithDecisions.put(JbSupportSkillFixHierarchyFields.botReply, summary);
		CcpJsonRepresentation finished = JbSupportSkillFixHierarchyStatus.reviewFinished.throwException(jsonWithSummary);
		return finished;
	}

	private static String getSkillsWithTheDecision(List<CcpJsonRepresentation> decisions, VisSkillFixHierarchyDecisions decision) {
		String decisionName = decision.name();
		Stream<CcpJsonRepresentation> decisionsStream = decisions.stream();
		Stream<CcpJsonRepresentation> withTheDecisionStream = decisionsStream.filter(item -> decisionName.equals(item.getAsString(VisSkillFixHierarchyReviewFields.decision)));
		Stream<String> skillsStream = withTheDecisionStream.map(item -> item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill));
		String skills = skillsStream.collect(Collectors.joining(", "));
		boolean noSkill = skills.isEmpty();
		String skillsOrDash = noSkill ? "-" : skills;
		return skillsOrDash;
	}
}
