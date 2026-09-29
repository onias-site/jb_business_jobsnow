package com.jb.business.bots.skill.hierarchy;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.business.skill.VisSkillFixHierarchyDecisions;

/**
 * What the operator typed in answer to a step of the {@code fixSkillHierarchy} command: a decision
 * ({@code aprovar}/{@code approve} or {@code rejeitar}/{@code reprovar}/{@code reject}) followed by the
 * justification, or {@code um a um}/{@code one by one} to decide item by item.
 */
final class JbSupportSkillFixHierarchyAnswer {

	final JbSupportSkillFixHierarchyAnswerType type;

	final String justification;

	private JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType type, String justification) {
		this.type = type;
		this.justification = justification;
	}

	static JbSupportSkillFixHierarchyAnswer read(CcpJsonRepresentation json) {

		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String answer = typedValue.trim();
		String lowerCaseAnswer = answer.toLowerCase();

		boolean isOneByOne = lowerCaseAnswer.equals("um a um") || lowerCaseAnswer.equals("one by one");

		if(isOneByOne) {
			JbSupportSkillFixHierarchyAnswer oneByOne = new JbSupportSkillFixHierarchyAnswer(JbSupportSkillFixHierarchyAnswerType.oneByOne, "");
			return oneByOne;
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

	boolean isOneByOne() {
		boolean oneByOne = JbSupportSkillFixHierarchyAnswerType.oneByOne == this.type;
		return oneByOne;
	}

	/**
	 * A decision counts only with its justification: it goes to the user in the email.
	 */
	boolean isDecisionWithJustification() {
		boolean isDecision = JbSupportSkillFixHierarchyAnswerType.approve == this.type || JbSupportSkillFixHierarchyAnswerType.reject == this.type;
		boolean hasJustification = false == this.justification.isEmpty();
		boolean decisionWithJustification = isDecision && hasJustification;
		return decisionWithJustification;
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
