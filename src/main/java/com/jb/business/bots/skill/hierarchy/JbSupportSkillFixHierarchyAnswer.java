package com.jb.business.bots.skill.hierarchy;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.business.skill.VisSkillFixHierarchyDecisions;

/**
 * What the operator typed in answer to a step of the {@code fixSkillHierarchy} command, in Portuguese or English: a
 * decision (approve or reject, with the Portuguese verbs aprovar, rejeitar or reprovar) followed by the justification;
 * one by one (um a um) to decide item by item; ignore (ignorar) to ignore the user for the command; and yes or no (sim,
 * nao with or without the accent) to confirm it. Case is ignored. Also read by the {@code reviewSkillSuggestion}
 * command, which takes the same decisions on a single skill.
 */
public final class JbSupportSkillFixHierarchyAnswer {

	/** The kind of answer. */
	final JbSupportSkillFixHierarchyAnswerType type;

	/** The justification of a decision; empty otherwise. */
	public final String justification;

	/**
	 * Builds the answer.
	 * @param type the kind of answer
	 * @param justification the justification
	 */
	private JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType type, String justification) {
		this.type = type;
		this.justification = justification;
	}

	/**
	 * Reads the answer from {@code typedValue}.
	 * @param json the session
	 * @return the answer ({@code notUnderstood} when it matches nothing)
	 */
	public static JbSupportSkillFixHierarchyAnswer read(CcpJsonRepresentation json) {

		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String answer = typedValue.trim();
		String lowerCaseAnswer = answer.toLowerCase();

		boolean isOneByOne = lowerCaseAnswer.equals("um a um") || lowerCaseAnswer.equals("one by one");

		if(isOneByOne) {
			JbSupportSkillFixHierarchyAnswer oneByOne = new JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType.oneByOne, "");
			return oneByOne;
		}

		boolean isIgnore = lowerCaseAnswer.equals("ignorar") || lowerCaseAnswer.equals("ignore");

		if(isIgnore) {
			JbSupportSkillFixHierarchyAnswer ignore = new JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType.ignore, "");
			return ignore;
		}

		boolean isYes = lowerCaseAnswer.equals("sim") || lowerCaseAnswer.equals("yes");

		if(isYes) {
			JbSupportSkillFixHierarchyAnswer yes = new JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType.yes, "");
			return yes;
		}

		boolean isNo = lowerCaseAnswer.equals("não") || lowerCaseAnswer.equals("nao") || lowerCaseAnswer.equals("no");

		if(isNo) {
			JbSupportSkillFixHierarchyAnswer no = new JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType.no, "");
			return no;
		}

		String[] firstWordAndTheRest = answer.split("\\s+", 2);
		String firstWord = firstWordAndTheRest[0].toLowerCase();
		boolean hasTheRest = firstWordAndTheRest.length > 1;
		String justification = hasTheRest ? firstWordAndTheRest[1].trim() : "";

		boolean isApproval = firstWord.equals("aprovar") || firstWord.equals("approve");

		if(isApproval) {
			JbSupportSkillFixHierarchyAnswer approval = new JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType.approve, justification);
			return approval;
		}

		boolean isRejection = firstWord.equals("rejeitar") || firstWord.equals("reprovar") || firstWord.equals("reject");

		if(isRejection) {
			JbSupportSkillFixHierarchyAnswer rejection = new JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType.reject, justification);
			return rejection;
		}

		JbSupportSkillFixHierarchyAnswer notUnderstood = new JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType.notUnderstood, "");
		return notUnderstood;
	}

	/**
	 * Tells whether the operator chose to decide item by item.
	 * @return {@code true} for one by one
	 */
	public boolean isOneByOne() {
		boolean oneByOne = JbSupportSkillFixHierarchyAnswerType.oneByOne == this.type;
		return oneByOne;
	}

	/**
	 * Tells whether the operator asked to ignore the user.
	 * @return {@code true} for ignore
	 */
	public boolean isIgnore() {
		boolean ignore = JbSupportSkillFixHierarchyAnswerType.ignore == this.type;
		return ignore;
	}

	/**
	 * Tells whether the operator confirmed.
	 * @return {@code true} for yes
	 */
	public boolean isYes() {
		boolean yes = JbSupportSkillFixHierarchyAnswerType.yes == this.type;
		return yes;
	}

	/**
	 * Tells whether the operator declined.
	 * @return {@code true} for no
	 */
	public boolean isNo() {
		boolean no = JbSupportSkillFixHierarchyAnswerType.no == this.type;
		return no;
	}

	/**
	 * A decision counts only with its justification: it goes to the user in the email.
	 */
	public boolean isDecisionWithJustification() {
		boolean isDecision = JbSupportSkillFixHierarchyAnswerType.approve == this.type || JbSupportSkillFixHierarchyAnswerType.reject == this.type;
		boolean hasJustification = false == this.justification.isEmpty();
		boolean decisionWithJustification = isDecision && hasJustification;
		return decisionWithJustification;
	}

	/**
	 * Tells whether the operator approved (with or without justification).
	 * @return {@code true} for an approval
	 */
	public boolean isApproval() {
		boolean approval = JbSupportSkillFixHierarchyAnswerType.approve == this.type;
		return approval;
	}

	/**
	 * The decision of an approval or of a rejection.
	 */
	VisSkillFixHierarchyDecisions getDecision() {
		boolean isApproval = JbSupportSkillFixHierarchyAnswerType.approve == this.type;
		VisSkillFixHierarchyDecisions decision = isApproval ? VisSkillFixHierarchyDecisions.approved : VisSkillFixHierarchyDecisions.rejected;
		return decision;
	}
}
