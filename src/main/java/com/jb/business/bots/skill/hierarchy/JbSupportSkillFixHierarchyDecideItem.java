package com.jb.business.bots.skill.hierarchy;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillFixHierarchyReview;
import com.vis.business.skill.VisSkillFixHierarchyReviewFields;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;

/**
 * Step of the one by one review of the {@code fixSkillHierarchy} command: the operator approves or rejects the
 * current item, always with a justification. The step repeats itself (its {@code nextStep} is itself) while
 * there are items left, asking for the next one; the decision on the last item finishes the review
 * ({@code reviewFinished}). An answer that is not a decision with justification asks again for the same item
 * ({@code invalidAnswer}).
 */
public class JbSupportSkillFixHierarchyDecideItem implements CcpBusiness {

	/**
	 * Records the decision on the current item and asks for the next one, or finishes the review after the last.
	 * @param json the session, with {@code itemIndex}
	 * @return the session asking for the next item
	 * @throws CcpErrorFlowDisturb with {@code reviewFinished} or {@code invalidAnswer}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		int itemIndex = json.getAsIntegerNumber(JbSupportSkillFixHierarchyFields.itemIndex);
		JbSupportSkillFixHierarchyAnswer answer = JbSupportSkillFixHierarchyAnswer.read(json);
		boolean decisionWithJustification = answer.isDecisionWithJustification();

		if(false == decisionWithJustification) {
			JnLanguage language = JbSupportSkillFixHierarchyConversation.getLanguage(json);
			String notUnderstood = JbSupportSkillFixHierarchyConversation.getNotUnderstood(language);
			String sameItemPrompt = JbSupportSkillFixHierarchyConversation.getItemPrompt(json, itemIndex);
			String askAgain = notUnderstood + sameItemPrompt;
			CcpJsonRepresentation jsonAskingAgain = json.put(JbSupportSkillFixHierarchyFields.botReply, askAgain);
			CcpJsonRepresentation askedAgain = JbSupportSkillFixHierarchyStatus.invalidAnswer.throwException(jsonAskingAgain);
			return askedAgain;
		}

		List<CcpJsonRepresentation> reviewItems = json.getAsJsonList(JbSupportSkillFixHierarchyFields.reviewItems);
		CcpJsonRepresentation item = reviewItems.get(itemIndex);
		String type = item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.type);
		String skill = item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
		CcpJsonRepresentation decision = VisBusinessSkillFixHierarchyReview.getDecision(type, skill, answer.getDecision(), answer.justification);

		List<CcpJsonRepresentation> previousDecisions = json.getAsJsonList(VisSkillFixHierarchyReviewFields.reviewDecisions);
		List<CcpJsonRepresentation> decisions = new ArrayList<>(previousDecisions);
		decisions.add(decision);

		int nextItemIndex = itemIndex + 1;
		boolean isLastItem = nextItemIndex >= reviewItems.size();

		if(isLastItem) {
			CcpJsonRepresentation finished = JbSupportSkillFixHierarchyConversation.finish(json, decisions);
			return finished;
		}

		CcpJsonRepresentation jsonWithDecisions = json.put(VisSkillFixHierarchyReviewFields.reviewDecisions, decisions);
		CcpJsonRepresentation jsonWithNextIndex = jsonWithDecisions.put(JbSupportSkillFixHierarchyFields.itemIndex, nextItemIndex);
		String nextItemPrompt = JbSupportSkillFixHierarchyConversation.getItemPrompt(jsonWithNextIndex, nextItemIndex);
		CcpJsonRepresentation jsonWithPrompt = jsonWithNextIndex.put(JbSupportSkillFixHierarchyFields.botReply, nextItemPrompt);
		return jsonWithPrompt;
	}
}
