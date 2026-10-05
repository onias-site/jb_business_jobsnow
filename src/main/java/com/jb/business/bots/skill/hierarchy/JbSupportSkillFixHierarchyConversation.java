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
 * session, and the end of the review, shared by the steps that can finish it. The texts themselves live in
 * {@link JbSupportSkillFixHierarchyMessages}.
 */
final class JbSupportSkillFixHierarchyConversation {

	/** Utility class; not instantiable. */
	private JbSupportSkillFixHierarchyConversation() {}

	/**
	 * Returns the language of the session (the enum itself or its name), Portuguese by default.
	 * @param json the session
	 * @return the language
	 */
	static JnLanguage getLanguage(CcpJsonRepresentation json) {
		// the language may be in the json as the enum itself or as its name
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		return language;
	}

	/**
	 * Tells whether the language is Portuguese.
	 * @param language the language
	 * @return {@code true} for Portuguese
	 */
	static boolean isPortuguese(JnLanguage language) {
		boolean portuguese = JnLanguage.portuguese == language;
		return portuguese;
	}

	/**
	 * Returns the options offered to the operator.
	 * @param language the language
	 * @return the options
	 */
	static String getOptions(JnLanguage language) {
		String options = JbSupportSkillFixHierarchyMessages.options.getMessage(language);
		return options;
	}

	/**
	 * Asks the operator to confirm that the user will be ignored for the command.
	 */
	static String getIgnoreConfirmation(CcpJsonRepresentation json) {
		JnLanguage language = getLanguage(json);
		CcpJsonRepresentation parameters = json.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email);
		String ignoreConfirmation = JbSupportSkillFixHierarchyMessages.ignoreConfirmation.getMessage(language, parameters);
		return ignoreConfirmation;
	}

	/**
	 * Justification that goes to the user for an item decided in an earlier review, which the operator is not
	 * asked about again.
	 */
	static String getPreviousDecisionJustification(JnLanguage language, VisSkillFixHierarchyDecisions decision) {
		boolean approved = VisSkillFixHierarchyDecisions.approved == decision;
		JbSupportSkillFixHierarchyMessages previousDecision = approved ? JbSupportSkillFixHierarchyMessages.previousDecisionApproved : JbSupportSkillFixHierarchyMessages.previousDecisionRejected;
		String previousDecisionJustification = previousDecision.getMessage(language);
		return previousDecisionJustification;
	}

	/**
	 * Returns the notice of an answer not understood.
	 * @param language the language
	 * @return the notice
	 */
	static String getNotUnderstood(JnLanguage language) {
		String notUnderstood = JbSupportSkillFixHierarchyMessages.notUnderstood.getMessage(language);
		return notUnderstood;
	}

	/**
	 * Asks the operator to decide the item at the given position.
	 */
	static String getItemPrompt(CcpJsonRepresentation json, int itemIndex) {

		JnLanguage language = getLanguage(json);
		List<CcpJsonRepresentation> reviewItems = json.getAsJsonList(JbSupportSkillFixHierarchyFields.reviewItems);
		CcpJsonRepresentation item = reviewItems.get(itemIndex);
		VisSkillFixHierarchyTypes type = item.getAsEnum(VisEntitySkillFixHierarchyItemPending.Fields.type, VisSkillFixHierarchyTypes.class);
		String typeDescription = type.getDescription(language);
		int itemNumber = itemIndex + 1;
		int itemsCount = reviewItems.size();

		String parent = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);
		CcpJsonRepresentation itemSkill = item.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.skill);
		CcpJsonRepresentation itemWithParent = itemSkill.put(VisEntitySkillFixHierarchyPending.Fields.parent, parent);

		CcpJsonRepresentation parameters = itemWithParent
				.put(JbSupportSkillFixHierarchyMessageFields.typeDescription, typeDescription)
				.put(JbSupportSkillFixHierarchyMessageFields.itemNumber, itemNumber)
				.put(JbSupportSkillFixHierarchyMessageFields.itemsCount, itemsCount);

		String itemPrompt = JbSupportSkillFixHierarchyMessages.itemPrompt.getMessage(language, parameters);
		return itemPrompt;
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
		String approvedSkills = getSkillsWithTheDecision(decisions, VisSkillFixHierarchyDecisions.approved, language);
		String rejectedSkills = getSkillsWithTheDecision(decisions, VisSkillFixHierarchyDecisions.rejected, language);

		CcpJsonRepresentation parameters = requestKey
				.put(JbSupportSkillFixHierarchyMessageFields.approvedSkills, approvedSkills)
				.put(JbSupportSkillFixHierarchyMessageFields.rejectedSkills, rejectedSkills);

		String summary = JbSupportSkillFixHierarchyMessages.reviewFinished.getMessage(language, parameters);

		CcpJsonRepresentation jsonWithDecisions = json.put(VisSkillFixHierarchyReviewFields.reviewDecisions, decisions);
		CcpJsonRepresentation jsonWithSummary = jsonWithDecisions.put(JbSupportSkillFixHierarchyFields.botReply, summary);
		CcpJsonRepresentation finished = JbSupportSkillFixHierarchyStatus.reviewFinished.throwException(jsonWithSummary);
		return finished;
	}

	/**
	 * Joins with commas the skills with the decision, or returns the text for none.
	 * @param decisions the decisions
	 * @param decision the decision
	 * @param language the language
	 * @return the skills
	 */
	private static String getSkillsWithTheDecision(List<CcpJsonRepresentation> decisions, VisSkillFixHierarchyDecisions decision, JnLanguage language) {
		String decisionName = decision.name();
		Stream<CcpJsonRepresentation> decisionsStream = decisions.stream();
		Stream<CcpJsonRepresentation> withTheDecisionStream = decisionsStream.filter(item -> decisionName.equals(item.getAsString(VisSkillFixHierarchyReviewFields.decision)));
		Stream<String> skillsStream = withTheDecisionStream.map(item -> item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill));
		String skills = skillsStream.collect(Collectors.joining(", "));
		boolean noSkill = skills.isEmpty();

		if(noSkill) {
			String noSkillText = JbSupportSkillFixHierarchyMessages.noSkill.getMessage(language);
			return noSkillText;
		}

		return skills;
	}
}
